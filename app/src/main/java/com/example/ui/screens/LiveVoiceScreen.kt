package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.LiveTranscriptEntry
import com.example.data.LiveVoiceUiState
import com.example.model.AppLanguage
import com.example.model.HaqLocalization
import com.example.ui.theme.ForestDeep
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MonoLabelStyle

@SuppressLint("MissingPermission")
@Composable
fun LiveVoiceScreen(
    liveVoiceState: LiveVoiceUiState,
    selectedLanguage: AppLanguage,
    activeSchemeCount: Int,
    onSelectLanguage: (AppLanguage) -> Unit,
    onStartLiveTalk: () -> Unit,
    onStopLiveTalk: () -> Unit,
    onSendQuickPrompt: (String) -> Unit,
    onMatchSchemesFromTalk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = remember(selectedLanguage) { HaqLocalization.strings(selectedLanguage) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartLiveTalk()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for real-time voice conversations.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val isLiveActive = liveVoiceState is LiveVoiceUiState.Active
    val isConnecting = liveVoiceState is LiveVoiceUiState.Connecting
    val transcripts = (liveVoiceState as? LiveVoiceUiState.Active)?.transcripts.orEmpty()
    val hasUserSpeech = transcripts.any { it.isUser }

    val quickPrompts = remember(selectedLanguage) {
        when (selectedLanguage) {
            AppLanguage.ENGLISH -> listOf(
                "I have 2 acres in Maharashtra, income ₹1.4 lakh",
                "Which subsidies support women farmers?",
                "What documents are needed for PM-KISAN?"
            )
            AppLanguage.HINDI -> listOf(
                "मेरे पास महाराष्ट्र में 2 एकड़ ज़मीन है, आय ₹1.4 लाख है",
                "महिला किसानों के लिए कौन सी सब्सिडी है?",
                "पीएम-किसान के लिए कौन से दस्तावेज़ चाहिए?"
            )
            AppLanguage.MARATHI -> listOf(
                "माझ्याकडे महाराष्ट्रात २ एकर शेती आहे, उत्पन्न ₹१.४ लाख आहे",
                "महिला शेतकऱ्यांसाठी कोणती अनुदान योजना आहे?",
                "पीएम-किसान साठी कोणती कागदपत्रे लागतात?"
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Live Voice Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ForestDeep)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(ForestEmerald, ForestDeep)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF4A261).copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color(0xFFF4A261).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "${strings.livePromoBadge} • $activeSchemeCount SCHEMES",
                                style = MonoLabelStyle,
                                color = Color(0xFFFFE5B4),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = strings.liveHeroTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = strings.liveHeroSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Animated Pulsing Live Voice Orb
                        LiveVoiceOrb(
                            isConnecting = isConnecting,
                            isActive = isLiveActive,
                            onClick = {
                                if (isLiveActive || isConnecting) {
                                    onStopLiveTalk()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        onStartLiveTalk()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        )

                        Text(
                            text = when (liveVoiceState) {
                                is LiveVoiceUiState.Connecting -> strings.liveConnectingText
                                is LiveVoiceUiState.Active -> strings.liveActiveHint
                                else -> "Powered by Gemini Live (gemini-3.8-live)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD8F3DC),
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                if (isLiveActive || isConnecting) {
                                    onStopLiveTalk()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        onStartLiveTalk()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiveActive) Color(0xFFC1121F) else Color(0xFFF4A261),
                                contentColor = if (isLiveActive) Color.White else ForestDeep
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("live_voice_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isLiveActive) Icons.Filled.CallEnd else Icons.Filled.Mic,
                                contentDescription = if (isLiveActive) strings.liveStopButton else strings.liveStartButton,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isConnecting -> strings.liveConnectingText
                                    isLiveActive -> strings.liveStopButton
                                    else -> strings.liveStartButton
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Language selector row
        item {
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
                        modifier = Modifier.testTag("live_lang_chip_${lang.name.lowercase()}")
                    )
                }
            }
        }

        // Real error banner (never swallowed)
        if (liveVoiceState is LiveVoiceUiState.Error) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = liveVoiceState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // Quick-tap conversation prompts when active
        if (isLiveActive) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tap to ask live or speak into your mic:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPrompts.forEach { prompt ->
                            AssistChip(
                                onClick = { onSendQuickPrompt(prompt) },
                                label = { Text(prompt, maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Actionable bridge from Live Voice conversation -> Structured Firestore Rule Matching
        if (hasUserSpeech) {
            item {
                Button(
                    onClick = onMatchSchemesFromTalk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("live_match_schemes_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.liveMatchFromTalkButton, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Real-time conversation transcripts
        if (transcripts.isNotEmpty()) {
            item {
                Text(
                    text = strings.liveTranscriptHeader,
                    style = MonoLabelStyle,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(items = transcripts, key = { it.id }) { entry ->
                LiveTranscriptBubble(entry = entry)
            }
        }

        // Zero-PII & Likely Eligible compliance notice
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                        text = "${strings.privacyGuardrail} ${strings.legalDisclaimer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun LiveVoiceOrb(
    isConnecting: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "live_orb_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActive) 1.24f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFFF4A261).copy(alpha = 0.24f))
            )
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isActive) Color(0xFFF4A261) else Color.White.copy(alpha = 0.16f),
            border = BorderStroke(2.dp, Color(0xFFFFE5B4).copy(alpha = 0.7f)),
            modifier = Modifier
                .size(88.dp)
                .testTag("live_voice_orb_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(34.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.GraphicEq else Icons.Filled.Mic,
                        contentDescription = if (isActive) "Active voice conversation" else "Start voice conversation",
                        tint = if (isActive) ForestDeep else Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTranscriptBubble(entry: LiveTranscriptEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (entry.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (entry.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (entry.isUser) 16.dp else 4.dp,
                bottomEnd = if (entry.isUser) 4.dp else 16.dp
            ),
            border = if (entry.isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (entry.isUser) "YOU" else "HAQ LIVE AI",
                    style = MonoLabelStyle,
                    color = if (entry.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (entry.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
