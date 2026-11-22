package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.audio.HaqAudioRecorder
import com.example.model.AppLanguage
import com.example.model.EligibilityRuleEngine
import com.example.model.EligibilityStatus
import com.example.model.FarmerProfileQuery
import com.example.model.FarmerScheme
import com.example.model.HaqLocalization
import com.example.model.HaqStrings
import com.example.model.InputMode
import com.example.model.JudgeDemoProfile
import com.example.model.PreSeededDemos
import com.example.model.SchemeMatchResult
import com.example.ui.theme.ForestDeep
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MonoLabelStyle
import com.example.viewmodel.AiParseUiState
import com.example.viewmodel.AudioTranscribeUiState
import com.example.viewmodel.SchemeGuideState
import com.example.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun FinderScreen(
    schemesState: UiState<List<FarmerScheme>>,
    plainInputText: String,
    aiParseState: AiParseUiState,
    audioTranscribeState: AudioTranscribeUiState = AudioTranscribeUiState.Idle,
    activeProfile: FarmerProfileQuery?,
    matchResults: List<SchemeMatchResult>,
    schemeGuides: Map<String, SchemeGuideState>,
    selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onRetrySync: () -> Unit = {},
    onPlainInputChange: (String) -> Unit,
    onAnalyzePlainQuery: (InputMode) -> Unit,
    onAudioRecordedForTranscription: (ByteArray, String) -> Unit = { _, _ -> },
    onOpenLiveVoice: () -> Unit = {},
    onRunDemoProfile: (JudgeDemoProfile) -> Unit,
    onExplainScheme: (FarmerScheme) -> Unit,
    onOpenMasterChecklist: (List<SchemeMatchResult>) -> Unit,
    onOpenFundingSources: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = remember(selectedLanguage) { HaqLocalization.strings(selectedLanguage) }
    val audioRecorder = remember(context) { HaqAudioRecorder(context.applicationContext) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    DisposableEffect(audioRecorder) {
        onDispose {
            audioRecorder.stopAndDiscard()
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isRecordingAudio) {
                delay(1000L)
                recordingSeconds += 1
            }
        }
    }

    fun startAudioCapture() {
        audioRecorder.startRecording().fold(
            onSuccess = { isRecordingAudio = true },
            onFailure = { err ->
                isRecordingAudio = false
                Toast.makeText(
                    context,
                    err.localizedMessage ?: "Microphone recording unavailable on this device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    fun stopAudioCaptureAndTranscribe() {
        isRecordingAudio = false
        audioRecorder.stopAndReadBytes().fold(
            onSuccess = { bytes ->
                onAudioRecordedForTranscription(bytes, "audio/mp4")
            },
            onFailure = { err ->
                Toast.makeText(
                    context,
                    err.localizedMessage ?: "Recording was too short. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startAudioCapture()
        } else {
            Toast.makeText(
                context,
                "Microphone permission denied. You can still type in plain language.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val activeSchemeCount = (schemesState as? UiState.Success)?.data?.count { it.active } ?: 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Editorial Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_farmer_1795319124973),
                    contentDescription = "Haq farmer fields banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ForestDeep.copy(alpha = 0.92f),
                                    ForestDeep.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color(0xFFF4A261).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, Color(0xFFF4A261).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = strings.activeSchemesBadge(activeSchemeCount),
                            style = MonoLabelStyle,
                            color = Color(0xFFFFE5B4),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.heroTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = strings.heroSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // 1B. Reframed Flagship AI Feature: Gemini Live Real-Time Voice Advisor Banner
        item {
            Card(
                onClick = onOpenLiveVoice,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ForestEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finder_live_voice_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFF4A261).copy(alpha = 0.22f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFFF4A261).copy(alpha = 0.7f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = "Haq Live Voice Advisor",
                                tint = Color(0xFFFFE5B4),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.livePromoBadge,
                            style = MonoLabelStyle,
                            color = Color(0xFFFFE5B4)
                        )
                        Text(
                            text = strings.livePromoTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.livePromoSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                    Surface(
                        color = Color(0xFFF4A261),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = strings.livePromoCta,
                            style = MaterialTheme.typography.labelMedium,
                            color = ForestDeep,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Zero-PII Privacy Guardrail Strip
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Zero PII",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.privacyGuardrail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 3. Pre-Seeded 5-Second Judge Demo Profiles (Quick Fix #1)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.quickDemoHeader,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                PreSeededDemos.profiles.forEach { demo ->
                    val demoTitle = when (demo.id) {
                        "demo_maharashtra_small" -> strings.demo1Title
                        "demo_female_equipment" -> strings.demo2Title
                        else -> demo.title
                    }
                    val demoSubtitle = when (demo.id) {
                        "demo_maharashtra_small" -> strings.demo1Subtitle
                        "demo_female_equipment" -> strings.demo2Subtitle
                        else -> demo.subtitle
                    }
                    OutlinedCard(
                        onClick = { onRunDemoProfile(demo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(demo.id),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.2.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = demoTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = demoSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = strings.testPill,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Plain-Language & Regional Dialect Input Card (Gap #1)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Gemini plain language understanding",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = strings.inputTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = strings.inputSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Live App Language + Voice Dialect selector chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { onSelectLanguage(lang) },
                                label = { Text(lang.chipLabel) },
                                modifier = Modifier.testTag("lang_chip_${lang.name.lowercase()}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = plainInputText,
                        onValueChange = onPlainInputChange,
                        placeholder = {
                            Text(
                                strings.inputPlaceholder,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plain_language_input")
                    )

                    if (audioTranscribeState is AudioTranscribeUiState.Success) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = strings.transcribedBadge,
                                style = MonoLabelStyle,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isTranscribing = audioTranscribeState is AudioTranscribeUiState.Transcribing
                        OutlinedButton(
                            onClick = {
                                if (isRecordingAudio) {
                                    stopAudioCaptureAndTranscribe()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        startAudioCapture()
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            enabled = !isTranscribing,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("voice_input_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.2.dp,
                                if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            if (isTranscribing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.transcribingButton)
                            } else {
                                Icon(
                                    imageVector = if (isRecordingAudio) Icons.Filled.Stop else Icons.Filled.Mic,
                                    contentDescription = if (isRecordingAudio) strings.stopRecordButton else strings.recordAudioButton,
                                    tint = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isRecordingAudio) {
                                        "${strings.stopRecordButton} (${recordingSeconds}s)"
                                    } else {
                                        strings.recordAudioButton
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = { onAnalyzePlainQuery(InputMode.PLAIN_LANGUAGE) },
                            enabled = aiParseState !is AiParseUiState.Parsing && !isRecordingAudio && !isTranscribing,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("find_benefits_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (aiParseState is AiParseUiState.Parsing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.understandingButton)
                            } else {
                                Text(
                                    text = strings.findBenefitsButton,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (audioTranscribeState is AudioTranscribeUiState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = audioTranscribeState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (aiParseState is AiParseUiState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = aiParseState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                OutlinedButton(
                                    onClick = { onAnalyzePlainQuery(InputMode.PLAIN_LANGUAGE) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("ai_parse_retry_button")
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Loading / Error state for Firestore Schemes
        when (schemesState) {
            is UiState.Loading -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Syncing live scheme rules from Firestore...")
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Could not load editable schemes: ${schemesState.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            OutlinedButton(
                                onClick = onRetrySync,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("finder_retry_sync_button")
                            ) {
                                Text("Retry Sync")
                            }
                        }
                    }
                }
                if (activeProfile != null) {
                    item {
                        ParsedProfileSummaryCard(
                            profile = activeProfile,
                            matchCount = 0,
                            strings = strings,
                            onDownloadChecklist = {}
                        )
                    }
                }
            }
            is UiState.Success -> {
                // Results section when a farmer profile has been analyzed or demo tapped
                if (activeProfile != null) {
                    item {
                        ParsedProfileSummaryCard(
                            profile = activeProfile,
                            matchCount = matchResults.size,
                            strings = strings,
                            onDownloadChecklist = { onOpenMasterChecklist(matchResults) }
                        )
                    }

                    // Guardrail #1: Explicit "Likely Eligible" Legal Framing
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Gavel,
                                    contentDescription = "Legal compliance notice",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.legalDisclaimer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (matchResults.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = strings.noMatchesTitle,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = strings.noMatchesSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = matchResults,
                            key = { it.scheme.id.ifBlank { it.scheme.shortName } }
                        ) { match ->
                            val guideKey = match.scheme.id.ifBlank { match.scheme.shortName }
                            AnimatedVisibility(visible = true, enter = fadeIn()) {
                                SchemeMatchCard(
                                    match = match,
                                    strings = strings,
                                    guideState = schemeGuides[guideKey] ?: SchemeGuideState.Idle,
                                    onExplainSimply = { onExplainScheme(match.scheme) },
                                    onOpenChecklist = { onOpenMasterChecklist(listOf(match)) },
                                    onOpenOfficialUrl = { url ->
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            )
                                        } catch (_: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Official portal: $url",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compact Funding Sources transparency card for judges on the home screen
        item {
            com.example.ui.components.FundingSourcesCompactCard(
                onExploreClick = onOpenFundingSources,
                strings = strings
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ParsedProfileSummaryCard(
    profile: FarmerProfileQuery,
    matchCount: Int,
    strings: HaqStrings,
    onDownloadChecklist: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = ForestEmerald
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.likelyEligibleCount(matchCount),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = profile.detectedLanguage.uppercase(),
                        style = MonoLabelStyle,
                        color = Color(0xFFFFE5B4),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (profile.aiUnderstandingNote.isNotBlank()) {
                Text(
                    text = profile.aiUnderstandingNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }

            // Extracted transparent criteria chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttributePill("State: ${profile.state}")
                AttributePill("Land: ${EligibilityRuleEngine.formatAcres(profile.landAcres)} Acres")
                AttributePill("Income: ${EligibilityRuleEngine.formatRupees(profile.annualIncomeInr)}/yr")
                AttributePill("Group: ${profile.gender.label}")
            }

            // Quick Fix #3: 1-Click Download Document Checklist Button
            if (matchCount > 0) {
                Button(
                    onClick = onDownloadChecklist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("download_document_checklist_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF4A261),
                        contentColor = ForestDeep
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = strings.downloadChecklistButton,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.downloadChecklistButton,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AttributePill(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            style = MonoLabelStyle,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SchemeMatchCard(
    match: SchemeMatchResult,
    strings: HaqStrings,
    guideState: SchemeGuideState,
    onExplainSimply: () -> Unit,
    onOpenChecklist: () -> Unit,
    onOpenOfficialUrl: (String) -> Unit
) {
    val scheme = match.scheme
    val isHighMatch = match.status == EligibilityStatus.LIKELY_ELIGIBLE

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scheme_result_card_${scheme.shortName.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top badges: Guardrail #1 "Likely Eligible" + Gap #3 Last Verified Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isHighMatch) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isHighMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isHighMatch) strings.likelyEligibleBadge else strings.partialVerifyBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isHighMatch) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }

                // Gap #3: Visible Last-Verified Timestamp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${strings.verifiedPrefix} ${scheme.lastVerifiedDate}",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Scheme title & benefit
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = scheme.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${scheme.department} • ${strings.auditedByPrefix} ${scheme.verifiedByOrg}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = scheme.benefitSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Quick Fix #2: Transparent Rule Logic Breakdown
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.whyYouMatchedHeader,
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    match.matchedRules.forEach { rule ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (rule.passed) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (rule.passed) "✓" else "!",
                                    style = MonoLabelStyle,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = rule.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = rule.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Streaming AI Plain-Language Application Guide
            when (guideState) {
                is SchemeGuideState.Idle -> Unit
                is SchemeGuideState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = strings.guideLoadingText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                is SchemeGuideState.Streaming, is SchemeGuideState.Success -> {
                    val text = when (guideState) {
                        is SchemeGuideState.Streaming -> guideState.text
                        is SchemeGuideState.Success -> guideState.text
                        else -> ""
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.guideHeaderText,
                                style = MonoLabelStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                is SchemeGuideState.Error -> {
                    Text(
                        text = guideState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Action row: Official Portal Link + Explain Simply (AI) + Single-Scheme Checklist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onOpenOfficialUrl(scheme.officialSourceUrl) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = strings.officialSourceButton,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = strings.officialSourceButton,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Button(
                    onClick = onExplainSimply,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = strings.applyGuideButton,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = strings.applyGuideButton,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                TextButton(
                    onClick = onOpenChecklist,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Checklist,
                        contentDescription = "Scheme document checklist",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
