package com.example.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HaqAiParserRepository
import com.example.data.HaqFirestoreRepository
import com.example.data.HaqLiveVoiceManager
import com.example.data.LiveVoiceUiState
import com.example.model.AppLanguage
import com.example.model.EligibilityCheckLog
import com.example.model.EligibilityRuleEngine
import com.example.model.FarmerProfileQuery
import com.example.model.FarmerScheme
import com.example.model.HaqLocalization
import com.example.model.InputMode
import com.example.model.JudgeDemoProfile
import com.example.model.PartnerFunding
import com.example.model.SchemeMatchResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "HaqViewModel"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiParseUiState {
    data object Idle : AiParseUiState
    data object Parsing : AiParseUiState
    data class Ready(val profile: FarmerProfileQuery) : AiParseUiState
    data class Error(val message: String) : AiParseUiState
}

sealed interface SchemeGuideState {
    data object Idle : SchemeGuideState
    data object Loading : SchemeGuideState
    data class Streaming(val text: String) : SchemeGuideState
    data class Success(val text: String) : SchemeGuideState
    data class Error(val message: String) : SchemeGuideState
}

sealed interface AudioTranscribeUiState {
    data object Idle : AudioTranscribeUiState
    data object Transcribing : AudioTranscribeUiState
    data class Success(val transcript: String) : AudioTranscribeUiState
    data class Error(val message: String) : AudioTranscribeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class HaqViewModel(
    private val firestoreRepository: HaqFirestoreRepository,
    private val aiRepository: HaqAiParserRepository,
    private val currentUserId: String
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val schemesState: StateFlow<UiState<List<FarmerScheme>>> = retryTrigger
        .flatMapLatest {
            firestoreRepository
                .observeSchemes(currentUserId)
                .map<List<FarmerScheme>, UiState<List<FarmerScheme>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing schemes", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load editable scheme store"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val checkLogsState: StateFlow<UiState<List<EligibilityCheckLog>>> = retryTrigger
        .flatMapLatest {
            firestoreRepository
                .observeCheckLogs(currentUserId)
                .map<List<EligibilityCheckLog>, UiState<List<EligibilityCheckLog>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing check logs", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load impact telemetry"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val partnersState: StateFlow<UiState<List<PartnerFunding>>> = retryTrigger
        .flatMapLatest {
            firestoreRepository
                .observePartnerFunding(currentUserId)
                .map<List<PartnerFunding>, UiState<List<PartnerFunding>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing partner funding", rootError)
                    emit(UiState.Error(rootError.message ?: "Failed to load partner sponsors"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _plainInputText = MutableStateFlow("")
    val plainInputText: StateFlow<String> = _plainInputText.asStateFlow()

    private val _aiParseState = MutableStateFlow<AiParseUiState>(AiParseUiState.Idle)
    val aiParseState: StateFlow<AiParseUiState> = _aiParseState.asStateFlow()

    private val _activeProfile = MutableStateFlow<FarmerProfileQuery?>(null)
    val activeProfile: StateFlow<FarmerProfileQuery?> = _activeProfile.asStateFlow()

    private val _activeLogId = MutableStateFlow<String?>(null)

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _schemeGuides = MutableStateFlow<Map<String, SchemeGuideState>>(emptyMap())
    val schemeGuides: StateFlow<Map<String, SchemeGuideState>> = _schemeGuides.asStateFlow()

    private val _audioTranscribeState = MutableStateFlow<AudioTranscribeUiState>(AudioTranscribeUiState.Idle)
    val audioTranscribeState: StateFlow<AudioTranscribeUiState> = _audioTranscribeState.asStateFlow()

    private val liveVoiceManager = HaqLiveVoiceManager(viewModelScope)
    val liveVoiceState: StateFlow<LiveVoiceUiState> = liveVoiceManager.state

    // Live reactive match computation: updates automatically whenever B2G Admin edits schemes or user switches language!
    val matchResults: StateFlow<List<SchemeMatchResult>> = combine(
        schemesState,
        activeProfile,
        selectedLanguage
    ) { schemesUi, profile, language ->
        if (profile == null || schemesUi !is UiState.Success) {
            emptyList()
        } else {
            EligibilityRuleEngine.evaluateAll(schemesUi.data, profile, language)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    init {
        seedWorkspace()
    }

    fun retryFirestoreSync() {
        retryTrigger.value += 1
        seedWorkspace()
    }

    private fun seedWorkspace() {
        viewModelScope.launch {
            val seedResult = firestoreRepository.seedDefaultsIfEmpty(currentUserId)
            seedResult.onFailure { err ->
                _statusBanner.value = "Sync note: ${err.localizedMessage}"
            }
        }
    }

    fun selectLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
        _schemeGuides.value = emptyMap()
    }

    fun updatePlainInput(text: String) {
        _plainInputText.value = text
        if (_audioTranscribeState.value is AudioTranscribeUiState.Error) {
            _audioTranscribeState.value = AudioTranscribeUiState.Idle
        }
    }

    /**
     * Transcribes recorded farmer microphone audio using `gemini-3.5-transcribe` via Firebase AI Logic,
     * fills the plain-language farm query box, and automatically matches active Firestore schemes.
     */
    fun transcribeAudioAndFindBenefits(audioBytes: ByteArray, mimeType: String = "audio/mp4") {
        val lang = _selectedLanguage.value
        viewModelScope.launch {
            _audioTranscribeState.value = AudioTranscribeUiState.Transcribing
            _aiParseState.value = AiParseUiState.Idle
            val result = aiRepository.transcribeFarmerAudio(audioBytes, mimeType, lang)
            result.fold(
                onSuccess = { transcript ->
                    _plainInputText.value = transcript
                    _audioTranscribeState.value = AudioTranscribeUiState.Success(transcript)
                    analyzePlainLanguageQuery(InputMode.VOICE_DIALECT)
                },
                onFailure = { error ->
                    _audioTranscribeState.value = AudioTranscribeUiState.Error(
                        error.localizedMessage ?: "Audio transcription failed. Please try recording again."
                    )
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startLiveVoiceConversation() {
        val currentSchemes = (schemesState.value as? UiState.Success)?.data.orEmpty()
        liveVoiceManager.startConversation(
            language = _selectedLanguage.value,
            activeSchemes = currentSchemes
        )
    }

    fun sendLiveQuickPrompt(prompt: String) {
        liveVoiceManager.sendQuickTextTurn(prompt)
    }

    fun stopLiveVoiceConversation() {
        liveVoiceManager.stopConversation()
    }

    fun applyLiveConversationToFinder(onNavigateToFinder: () -> Unit) {
        val activeState = liveVoiceState.value as? LiveVoiceUiState.Active ?: return
        val userSummary = activeState.transcripts
            .filter { it.isUser }
            .joinToString(" ") { it.text }
            .trim()
        if (userSummary.isBlank()) {
            _statusBanner.value = "Speak about your farm first so Haq can match schemes."
            return
        }
        liveVoiceManager.stopConversation()
        _plainInputText.value = userSummary
        _audioTranscribeState.value = AudioTranscribeUiState.Success(userSummary)
        onNavigateToFinder()
        analyzePlainLanguageQuery(InputMode.VOICE_DIALECT)
    }

    override fun onCleared() {
        liveVoiceManager.closeNow()
        super.onCleared()
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearActiveResults() {
        _activeProfile.value = null
        _aiParseState.value = AiParseUiState.Idle
        _schemeGuides.value = emptyMap()
    }

    fun analyzePlainLanguageQuery(inputMode: InputMode = InputMode.PLAIN_LANGUAGE) {
        val raw = _plainInputText.value.trim()
        val lang = _selectedLanguage.value
        if (raw.isBlank()) {
            _aiParseState.value = AiParseUiState.Error(HaqLocalization.strings(lang).emptyQueryError)
            return
        }
        viewModelScope.launch {
            _aiParseState.value = AiParseUiState.Parsing
            _schemeGuides.value = emptyMap()
            val result = aiRepository.parsePlainLanguageFarmerQuery(raw, inputMode, lang)
            result.fold(
                onSuccess = { parsedProfile ->
                    _activeProfile.value = parsedProfile
                    _aiParseState.value = AiParseUiState.Ready(parsedProfile)
                    val currentSchemes = (schemesState.value as? UiState.Success)?.data.orEmpty()
                    val matchedCount = EligibilityRuleEngine.evaluateAll(currentSchemes, parsedProfile, lang).size
                    logMatchEvent(parsedProfile, matchedCount)
                },
                onFailure = { error ->
                    _aiParseState.value = AiParseUiState.Error(
                        error.localizedMessage ?: "Could not parse your query with Gemini AI. Please retry."
                    )
                }
            )
        }
    }

    fun runDemoProfile(demo: JudgeDemoProfile) {
        _plainInputText.value = demo.sampleDialectInput
        _activeProfile.value = demo.profileQuery
        _aiParseState.value = AiParseUiState.Ready(demo.profileQuery)
        _schemeGuides.value = emptyMap()

        viewModelScope.launch {
            val currentSchemes = (schemesState.value as? UiState.Success)?.data.orEmpty()
            val matchedCount = EligibilityRuleEngine.evaluateAll(
                currentSchemes,
                demo.profileQuery,
                _selectedLanguage.value
            ).size
            logMatchEvent(demo.profileQuery, matchedCount)
        }
    }

    fun requestPlainLanguageGuide(scheme: FarmerScheme) {
        val profile = _activeProfile.value ?: return
        val key = scheme.id.ifBlank { scheme.shortName }
        val lang = _selectedLanguage.value
        viewModelScope.launch {
            _schemeGuides.value = _schemeGuides.value + (key to SchemeGuideState.Loading)
            var latestText = ""
            try {
                aiRepository.streamPlainLanguageSchemeGuide(scheme, profile, lang).collect { partial ->
                    latestText = partial
                    _schemeGuides.value = _schemeGuides.value + (key to SchemeGuideState.Streaming(partial))
                }
                _schemeGuides.value = _schemeGuides.value + (key to SchemeGuideState.Success(latestText))
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI scheme guide timed out", e)
                _schemeGuides.value = _schemeGuides.value + (
                    key to SchemeGuideState.Error("AI guide timed out. Please try again.")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI scheme guide failed", e)
                _schemeGuides.value = _schemeGuides.value + (
                    key to SchemeGuideState.Error(e.localizedMessage ?: "Failed to generate guide.")
                )
            }
        }
    }

    fun recordChecklistDownloaded() {
        val logId = _activeLogId.value
        viewModelScope.launch {
            if (!logId.isNullOrBlank()) {
                firestoreRepository.markChecklistDownloaded(currentUserId, logId)
            } else {
                val profile = _activeProfile.value
                val logResult = firestoreRepository.logEligibilityCheck(
                    userId = currentUserId,
                    stateRegion = profile?.state ?: "Maharashtra",
                    inputMode = profile?.inputMode ?: InputMode.PLAIN_LANGUAGE,
                    matchedCount = matchResults.value.size
                )
                logResult.getOrNull()?.let { newId ->
                    _activeLogId.value = newId
                    firestoreRepository.markChecklistDownloaded(currentUserId, newId)
                }
            }
            _statusBanner.value = "Document checklist ready & logged (Zero PII stored)."
        }
    }

    private suspend fun logMatchEvent(profile: FarmerProfileQuery, matchedCount: Int) {
        val res = firestoreRepository.logEligibilityCheck(
            userId = currentUserId,
            stateRegion = profile.state,
            inputMode = profile.inputMode,
            matchedCount = matchedCount
        )
        res.onSuccess { id -> _activeLogId.value = id }
    }

    // --- B2B / B2G Admin Actions ---

    fun saveScheme(existingId: String?, scheme: FarmerScheme, onDone: () -> Unit) {
        viewModelScope.launch {
            val result = if (existingId.isNullOrBlank()) {
                firestoreRepository.createScheme(currentUserId, scheme)
            } else {
                firestoreRepository.updateScheme(currentUserId, existingId, scheme)
            }
            result.fold(
                onSuccess = {
                    _statusBanner.value = if (existingId.isNullOrBlank()) {
                        "Added '${scheme.shortName}' to live editable scheme store."
                    } else {
                        "Updated '${scheme.shortName}' rules live in Firestore."
                    }
                    onDone()
                },
                onFailure = { err ->
                    _statusBanner.value = "Save failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun markSchemeVerifiedToday(scheme: FarmerScheme, verifiedByOrg: String = "KVK Official Admin") {
        viewModelScope.launch {
            val result = firestoreRepository.markSchemeVerifiedToday(
                userId = currentUserId,
                schemeId = scheme.id,
                verifiedByOrg = verifiedByOrg
            )
            result.fold(
                onSuccess = {
                    _statusBanner.value = "Verified '${scheme.shortName}' for ${EligibilityRuleEngine.todayFormattedDate()}."
                },
                onFailure = { err ->
                    _statusBanner.value = "Verification update failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun toggleSchemeActive(scheme: FarmerScheme) {
        viewModelScope.launch {
            val nextState = !scheme.active
            val result = firestoreRepository.toggleSchemeActive(currentUserId, scheme.id, nextState)
            result.fold(
                onSuccess = {
                    _statusBanner.value = if (nextState) {
                        "Activated '${scheme.shortName}' for farmer matching."
                    } else {
                        "Paused '${scheme.shortName}'."
                    }
                },
                onFailure = { err ->
                    _statusBanner.value = "Status update failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun deleteScheme(scheme: FarmerScheme) {
        viewModelScope.launch {
            val result = firestoreRepository.deleteScheme(currentUserId, scheme.id)
            result.fold(
                onSuccess = {
                    _statusBanner.value = "Removed '${scheme.shortName}' from editable store."
                },
                onFailure = { err ->
                    _statusBanner.value = "Delete failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun addPartnerFunding(partner: PartnerFunding, onDone: () -> Unit) {
        viewModelScope.launch {
            val result = firestoreRepository.addPartnerFunding(currentUserId, partner)
            result.fold(
                onSuccess = {
                    _statusBanner.value = "Added '${partner.organizationName}' as institutional sponsor."
                    onDone()
                },
                onFailure = { err ->
                    _statusBanner.value = "Could not add sponsor: ${err.localizedMessage}"
                }
            )
        }
    }
}
