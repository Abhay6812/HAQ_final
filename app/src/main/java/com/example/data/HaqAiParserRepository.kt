package com.example.data

import android.util.Log
import com.example.model.AppLanguage
import com.example.model.FarmerProfileQuery
import com.example.model.FarmerScheme
import com.example.model.InputMode
import com.example.model.TargetGender
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "HaqAiParser"

class HaqAiParserRepository {

    private val farmerQuerySchema = Schema.obj(
        properties = mapOf(
            "detectedLanguage" to Schema.string(
                description = "Detected language or dialect (e.g., English, Hindi, Marathi, Hinglish, Tamil, Telugu, Gujarati, Punjabi)"
            ),
            "state" to Schema.string(
                description = "Indian state name (e.g., Maharashtra, Punjab, Uttar Pradesh, Madhya Pradesh, Gujarat, Karnataka). Default to Maharashtra if unspecified."
            ),
            "landAcres" to Schema.double(
                description = "Cultivable landholding converted to acres (1 hectare = 2.47 acres, 1 guntha = 0.025 acres, 1 bigha = 0.62 acres). Default to 2.0 if unspecified."
            ),
            "annualIncomeInr" to Schema.integer(
                description = "Annual family income in Indian Rupees (e.g., 1.5 lakh = 150000). Default to 150000 if unspecified."
            ),
            "gender" to Schema.enumeration(
                values = listOf("ALL", "FEMALE", "MALE"),
                description = "Farmer gender if explicitly mentioned (FEMALE for mahila/woman/she, MALE for man/he, otherwise ALL)."
            ),
            "primaryNeed" to Schema.enumeration(
                values = listOf(
                    "GENERAL",
                    "INCOME_SUPPORT",
                    "EQUIPMENT_SUBSIDY",
                    "CROP_INSURANCE",
                    "SOLAR_IRRIGATION",
                    "CREDIT_LOAN",
                    "WOMEN_FARMER"
                ),
                description = "Primary agricultural welfare need inferred from the farmer's statement."
            ),
            "plainLanguageSummary" to Schema.string(
                description = "One warm, clear, respectful sentence in the user's requested language confirming what Haq understood without repeating any sensitive personal numbers."
            )
        )
    )

    // High-volume Gemini 3.x Flash-Lite model: fast, billing not required, higher free-tier daily quota
    private val structuredParserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 512
                responseMimeType = "application/json"
                responseSchema = farmerQuerySchema
            },
            requestOptions = RequestOptions(timeoutInMillis = 25_000L)
        )
    }

    private val guideModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            requestOptions = RequestOptions(timeoutInMillis = 45_000L)
        )
    }

    private val transcribeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-transcribe",
            requestOptions = RequestOptions(timeoutInMillis = 45_000L)
        )
    }

    /**
     * Transcribes recorded farmer microphone audio via Firebase AI Logic (`gemini-3.5-transcribe`).
     * Supports English, Hindi, Marathi, and mixed regional dialects while stripping accidental PII numbers.
     */
    suspend fun transcribeFarmerAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        appLanguage: AppLanguage = AppLanguage.ENGLISH
    ): Result<String> {
        return try {
            require(audioBytes.isNotEmpty()) { "Audio recording is empty." }
            require(audioBytes.size < 18 * 1024 * 1024) { "Audio recording exceeds 18 MB limit." }

            val prompt = content {
                inlineData(audioBytes, mimeType)
                text(
                    buildString {
                        appendLine("Transcribe this farmer's voice note accurately into clean text.")
                        appendLine("Preserve their spoken language or dialect (preferred script: ${appLanguage.aiLanguageName}).")
                        appendLine("PRIVACY GUARDRAIL: If the speaker accidentally reads out a 12-digit Aadhaar number or bank account number, replace those digits with [REDACTED].")
                        appendLine("Return ONLY the clean transcription text without extra commentary or quotation marks.")
                    }
                )
            }

            val response = withContext(Dispatchers.IO) {
                withTimeout(55_000L) {
                    transcribeModel.generateContent(prompt)
                }
            }
            val transcript = response.text?.trim().orEmpty()
            if (transcript.isBlank()) {
                throw IllegalStateException("Audio transcription returned empty text. Please speak clearly and retry.")
            }
            Result.success(transcript)
        } catch (e: RequestTimeoutException) {
            Log.e(TAG, "Audio transcription request timed out", e)
            Result.failure(
                IllegalStateException("Audio transcription timed out. Please check your connection and retry.")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out", e)
            Result.failure(
                IllegalStateException("Audio transcription timed out. Please check your connection and retry.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            Result.failure(e)
        }
    }

    suspend fun parsePlainLanguageFarmerQuery(
        rawText: String,
        inputMode: InputMode,
        appLanguage: AppLanguage = AppLanguage.ENGLISH
    ): Result<FarmerProfileQuery> {
        return try {
            val prompt = buildString {
                appendLine("You are Haq, an empathetic Indian agricultural welfare assistant.")
                appendLine("Extract non-PII eligibility parameters from the farmer's plain-language or regional dialect description.")
                appendLine("STRICT PRIVACY GUARDRAIL: Completely ignore and NEVER repeat any Aadhaar numbers, phone numbers, bank account numbers, or passbook IDs if accidentally typed.")
                appendLine("Convert regional Indian land units (hectare, bigha, guntha, vigha) accurately to acres, and Lakh/Hazar amounts to exact INR integers.")
                appendLine("Write the 'plainLanguageSummary' field in ${appLanguage.aiLanguageName} so the farmer can read it comfortably in their chosen language.")
                appendLine()
                appendLine("Farmer's plain-language input:")
                appendLine(rawText)
            }

            // Execute on IO dispatcher with 1 automatic retry if the first HTTP socket stalls
            val response = withContext(Dispatchers.IO) {
                var lastTimeout: Exception? = null
                var result: com.google.firebase.ai.type.GenerateContentResponse? = null
                for (attempt in 1..2) {
                    try {
                        val attemptTimeoutMs = 28_000L
                        result = withTimeout(attemptTimeoutMs) {
                            structuredParserModel.generateContent(prompt)
                        }
                        break
                    } catch (e: RequestTimeoutException) {
                        Log.w(TAG, "Plain-language parse attempt $attempt timed out (SDK)", e)
                        lastTimeout = e
                    } catch (e: TimeoutCancellationException) {
                        Log.w(TAG, "Plain-language parse attempt $attempt timed out (coroutine)", e)
                        lastTimeout = e
                    }
                }
                result ?: throw (lastTimeout ?: IllegalStateException("AI parser timed out."))
            }

            val jsonString = response.text?.trim()
                ?: throw IllegalStateException("AI parser returned an empty response.")

            val json = JSONObject(jsonString)
            val rawLand = json.optDouble("landAcres", 2.0).let { if (it.isNaN() || it.isInfinite()) 2.0 else it }
            val parsed = FarmerProfileQuery(
                rawPlainText = rawText.trim(),
                detectedLanguage = json.optString("detectedLanguage", appLanguage.chipLabel).ifBlank { appLanguage.chipLabel },
                state = json.optString("state", "Maharashtra").ifBlank { "Maharashtra" },
                landAcres = rawLand.coerceIn(0.1, 500.0),
                annualIncomeInr = json.optLong("annualIncomeInr", 150000L).coerceIn(10000L, 50_000_000L),
                gender = TargetGender.fromRaw(json.optString("gender", "ALL")),
                primaryNeed = json.optString("primaryNeed", "GENERAL").ifBlank { "GENERAL" },
                aiUnderstandingNote = json.optString(
                    "plainLanguageSummary",
                    "Extracted your farming details to match active welfare rules."
                ),
                inputMode = inputMode
            )
            Result.success(parsed)
        } catch (e: RequestTimeoutException) {
            Log.e(TAG, "Plain-language parsing timed out (SDK)", e)
            Result.failure(
                IllegalStateException("AI understanding timed out on slow network. Please tap Retry.")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Plain-language parsing timed out", e)
            Result.failure(
                IllegalStateException("AI understanding timed out on slow network. Please tap Retry.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Plain-language parsing failed", e)
            Result.failure(e)
        }
    }

    fun streamPlainLanguageSchemeGuide(
        scheme: FarmerScheme,
        profile: FarmerProfileQuery,
        appLanguage: AppLanguage = AppLanguage.ENGLISH
    ): Flow<String> = flow {
        val prompt = buildString {
            appendLine("You are Haq, an honest, plain-language guide helping Indian farmers claim public welfare without paying middlemen.")
            appendLine("IMPORTANT: Write your entire response in ${appLanguage.aiLanguageName}.")
            appendLine("Explain in 4 short, clear bullet points how this farmer can apply for '${scheme.name}' (${scheme.department}).")
            appendLine("Farmer context: State=${profile.state}, Land=${profile.landAcres} acres.")
            appendLine("Benefit: ${scheme.benefitSummary}")
            appendLine("Official source: ${scheme.officialSourceUrl}")
            appendLine("Required documents: ${scheme.requiredDocuments.joinToString(", ")}")
            appendLine("MANDATORY RULES:")
            appendLine("1. Explicitly say eligibility is 'Likely Eligible' / 'संभाव्य पात्र' (NEVER say 'Guaranteed').")
            appendLine("2. Tell them they can apply directly at their nearest CSC, KVK, or official portal for ₹0 middleman fees.")
            appendLine("3. Remind them NEVER to share Aadhaar OTP or bank details with unauthorized agents.")
            appendLine("4. Keep total response concise (under 130 words), warm, and jargon-free.")
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            guideModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI guide returned an empty response.")
        }
    }
}
