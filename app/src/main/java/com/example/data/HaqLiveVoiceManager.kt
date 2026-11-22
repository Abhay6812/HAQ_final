package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.model.AppLanguage
import com.example.model.FarmerScheme
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "HaqLiveVoiceManager"

data class LiveTranscriptEntry(
    val id: Long,
    val isUser: Boolean,
    val text: String
)

sealed interface LiveVoiceUiState {
    data object Idle : LiveVoiceUiState
    data object Connecting : LiveVoiceUiState
    data class Active(
        val transcripts: List<LiveTranscriptEntry> = emptyList(),
        val activeLanguage: AppLanguage = AppLanguage.ENGLISH
    ) : LiveVoiceUiState
    data class Error(val message: String) : LiveVoiceUiState
}

/**
 * Real-time bidirectional voice conversation manager powered by Firebase AI Logic Live API (`gemini-3.8-live`).
 */
@OptIn(PublicPreviewAPI::class)
class HaqLiveVoiceManager(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<LiveVoiceUiState>(LiveVoiceUiState.Idle)
    val state: StateFlow<LiveVoiceUiState> = _state.asStateFlow()

    private var activeSession: LiveSession? = null
    private var nextEntryId = 1L

    private fun buildSystemInstruction(
        language: AppLanguage,
        activeSchemes: List<FarmerScheme>
    ) = content {
        val schemeDigest = activeSchemes
            .filter { it.active }
            .take(12)
            .joinToString("\n") { scheme ->
                "- ${scheme.name} (${scheme.shortName}): ${scheme.benefitSummary} | States: ${scheme.eligibleStates.joinToString(",")} | Max Land: ${scheme.maxLandAcres} acres | Max Income: ₹${scheme.maxAnnualIncomeInr} | Gender: ${scheme.targetGender} | Verified: ${scheme.lastVerifiedDate} | Portal: ${scheme.officialSourceUrl}"
            }

        text(
            buildString {
                appendLine("You are Haq Voice Advisor, a warm, trustworthy real-time voice guide helping Indian farmers discover public agricultural welfare schemes.")
                appendLine("Speak naturally and concisely in ${language.aiLanguageName}. If the farmer speaks Hindi, Marathi, Hinglish, or English, respond naturally in their language.")
                appendLine("MANDATORY GUARDRAILS:")
                appendLine("1. Always describe eligibility strictly as 'Likely Eligible' / 'संभाव्य पात्र' — NEVER say 'Guaranteed'.")
                appendLine("2. ZERO-PII: NEVER ask for Aadhaar numbers, bank account numbers, OTPs, or land passbook numbers. If a farmer mentions them, gently remind them not to share private numbers.")
                appendLine("3. ZERO FEES: Remind farmers that Haq is ₹0 free (funded by State Agriculture Depts, CSR Agri Funds, and Seed Co-ops) and they never need to pay middlemen.")
                appendLine("4. Ask simple conversational follow-up questions (state, approximate land acres, annual family income, crop/equipment need) and mention required documents like 7/12 extract or income certificate.")
                appendLine("5. Keep each spoken turn warm, clear, and under 35 seconds.")
                if (schemeDigest.isNotBlank()) {
                    appendLine()
                    appendLine("LIVE VERIFIED SCHEMES IN HAQ DATABASE:")
                    appendLine(schemeDigest)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startConversation(
        language: AppLanguage,
        activeSchemes: List<FarmerScheme>
    ) {
        scope.launch {
            stopInternal()
            _state.value = LiveVoiceUiState.Connecting
            try {
                val liveModel = Firebase.ai.liveModel(
                    modelName = "gemini-3.8-live",
                    generationConfig = liveGenerationConfig {
                        responseModality = ResponseModality.AUDIO
                        speechConfig = SpeechConfig(voice = Voice("AOEDE"))
                        inputAudioTranscription = AudioTranscriptionConfig()
                        outputAudioTranscription = AudioTranscriptionConfig()
                    },
                    systemInstruction = buildSystemInstruction(language, activeSchemes)
                )

                val session = withTimeout(30_000L) {
                    withContext(Dispatchers.IO) {
                        liveModel.connect()
                    }
                }
                activeSession = session
                _state.value = LiveVoiceUiState.Active(
                    transcripts = emptyList(),
                    activeLanguage = language
                )

                withContext(Dispatchers.IO) {
                    session.startAudioConversation(
                        transcriptHandler = { inputTranscription, outputTranscription ->
                            inputTranscription?.text?.trim()?.takeIf { it.isNotEmpty() }?.let { userText ->
                                appendTranscript(isUser = true, chunk = userText)
                            }
                            outputTranscription?.text?.trim()?.takeIf { it.isNotEmpty() }?.let { modelText ->
                                appendTranscript(isUser = false, chunk = modelText)
                            }
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Live voice session failed", e)
                stopInternal()
                _state.value = LiveVoiceUiState.Error(
                    e.localizedMessage ?: "Could not connect to Gemini Live voice conversation. Please retry."
                )
            }
        }
    }

    fun sendQuickTextTurn(prompt: String) {
        val session = activeSession ?: return
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        appendTranscript(isUser = true, chunk = clean)
        scope.launch(Dispatchers.IO) {
            try {
                session.send(content("user") { text(clean) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send live prompt", e)
            }
        }
    }

    fun stopConversation() {
        scope.launch {
            stopInternal()
            _state.value = LiveVoiceUiState.Idle
        }
    }

    fun closeNow() {
        val session = activeSession
        activeSession = null
        if (session != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    session.stopAudioConversation()
                } catch (_: Exception) {
                }
                try {
                    session.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun stopInternal() {
        val session = activeSession
        activeSession = null
        if (session != null) {
            withContext(Dispatchers.IO) {
                try {
                    session.stopAudioConversation()
                } catch (e: Exception) {
                    Log.w(TAG, "stopAudioConversation warning: ${e.message}")
                }
                try {
                    session.close()
                } catch (e: Exception) {
                    Log.w(TAG, "LiveSession close warning: ${e.message}")
                }
            }
        }
    }

    private fun appendTranscript(isUser: Boolean, chunk: String) {
        val current = _state.value as? LiveVoiceUiState.Active ?: return
        val list = current.transcripts.toMutableList()
        val last = list.lastOrNull()
        if (last != null && last.isUser == isUser && last.text.length < 280) {
            val merged = if (last.text.endsWith(chunk)) last.text else "${last.text} $chunk"
            list[list.lastIndex] = last.copy(text = merged.trim())
        } else {
            list.add(LiveTranscriptEntry(id = nextEntryId++, isUser = isUser, text = chunk))
        }
        _state.value = current.copy(transcripts = list.takeLast(30))
    }
}
