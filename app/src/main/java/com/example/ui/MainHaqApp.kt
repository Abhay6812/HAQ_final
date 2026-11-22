package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.HaqAiParserRepository
import com.example.data.HaqFirestoreRepository
import com.example.firebase.signOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import com.example.model.AppLanguage
import com.example.model.FarmerScheme
import com.example.model.HaqLocalization
import com.example.model.HaqStrings
import com.example.model.SchemeMatchResult
import com.example.ui.components.DocumentChecklistSheet
import com.example.ui.components.SchemeEditorSheet
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.BusinessModelScreen
import com.example.ui.screens.FinderScreen
import com.example.ui.screens.LiveVoiceScreen
import com.example.viewmodel.HaqViewModel
import com.example.viewmodel.UiState
import com.google.firebase.firestore.FirebaseFirestore

private enum class HaqTab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    FINDER(Icons.Filled.Agriculture, Icons.Outlined.Agriculture, "nav_tab_finder"),
    LIVE_VOICE(Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq, "nav_tab_live_voice"),
    ADMIN(Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_tab_admin"),
    FUNDING(Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance, "nav_tab_funding");

    fun label(strings: HaqStrings): String = when (this) {
        FINDER -> strings.tabFinder
        LIVE_VOICE -> strings.tabLiveVoice
        ADMIN -> strings.tabAdmin
        FUNDING -> strings.tabFunding
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHaqApp(
    currentUserId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: HaqViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HaqViewModel(
                    firestoreRepository = HaqFirestoreRepository(db),
                    aiRepository = HaqAiParserRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val schemesState by viewModel.schemesState.collectAsStateWithLifecycle()
    val checkLogsState by viewModel.checkLogsState.collectAsStateWithLifecycle()
    val partnersState by viewModel.partnersState.collectAsStateWithLifecycle()
    val plainInputText by viewModel.plainInputText.collectAsStateWithLifecycle()
    val aiParseState by viewModel.aiParseState.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val matchResults by viewModel.matchResults.collectAsStateWithLifecycle()
    val schemeGuides by viewModel.schemeGuides.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val audioTranscribeState by viewModel.audioTranscribeState.collectAsStateWithLifecycle()
    val liveVoiceState by viewModel.liveVoiceState.collectAsStateWithLifecycle()
    val strings = remember(selectedLanguage) { HaqLocalization.strings(selectedLanguage) }
    val activeSchemeCount = (schemesState as? UiState.Success)?.data?.count { it.active } ?: 0

    var currentTab by rememberSaveable { mutableStateOf(HaqTab.FINDER) }
    var editingScheme by remember { mutableStateOf<FarmerScheme?>(null) }
    var showSchemeEditor by remember { mutableStateOf(false) }
    var checklistMatches by remember { mutableStateOf<List<SchemeMatchResult>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    // Support system back gesture from secondary tabs to Home (Finder)
    BackHandler(enabled = currentTab != HaqTab.FINDER) {
        currentTab = HaqTab.FINDER
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                HaqTab.FINDER -> strings.topBarFinder
                                HaqTab.LIVE_VOICE -> strings.topBarLiveVoice
                                HaqTab.ADMIN -> strings.topBarAdmin
                                HaqTab.FUNDING -> strings.topBarFunding
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    },
                    actions = {
                        // 1-tap global language cycle pill (EN -> हिंदी -> मराठी)
                        Surface(
                            onClick = {
                                val all = AppLanguage.entries
                                val next = all[(all.indexOf(selectedLanguage) + 1) % all.size]
                                viewModel.selectLanguage(next)
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("top_bar_language_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Translate,
                                    contentDescription = "Switch language",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = selectedLanguage.shortBadge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                signOut(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        HaqTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val tabLabel = tab.label(strings)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tabLabel
                                    )
                                },
                                label = { Text(tabLabel) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        HaqTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val tabLabel = tab.label(strings)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tabLabel
                                    )
                                },
                                label = { Text(tabLabel) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)

                    when (currentTab) {
                        HaqTab.FINDER -> FinderScreen(
                            schemesState = schemesState,
                            plainInputText = plainInputText,
                            aiParseState = aiParseState,
                            audioTranscribeState = audioTranscribeState,
                            activeProfile = activeProfile,
                            matchResults = matchResults,
                            schemeGuides = schemeGuides,
                            selectedLanguage = selectedLanguage,
                            onSelectLanguage = viewModel::selectLanguage,
                            onRetrySync = viewModel::retryFirestoreSync,
                            onPlainInputChange = viewModel::updatePlainInput,
                            onAnalyzePlainQuery = viewModel::analyzePlainLanguageQuery,
                            onAudioRecordedForTranscription = viewModel::transcribeAudioAndFindBenefits,
                            onOpenLiveVoice = { currentTab = HaqTab.LIVE_VOICE },
                            onRunDemoProfile = viewModel::runDemoProfile,
                            onExplainScheme = viewModel::requestPlainLanguageGuide,
                            onOpenMasterChecklist = { selectedMatches ->
                                checklistMatches = selectedMatches
                            },
                            onOpenFundingSources = {
                                currentTab = HaqTab.FUNDING
                            },
                            modifier = contentModifier
                        )

                        HaqTab.LIVE_VOICE -> LiveVoiceScreen(
                            liveVoiceState = liveVoiceState,
                            selectedLanguage = selectedLanguage,
                            activeSchemeCount = activeSchemeCount,
                            onSelectLanguage = viewModel::selectLanguage,
                            onStartLiveTalk = viewModel::startLiveVoiceConversation,
                            onStopLiveTalk = viewModel::stopLiveVoiceConversation,
                            onSendQuickPrompt = viewModel::sendLiveQuickPrompt,
                            onMatchSchemesFromTalk = {
                                viewModel.applyLiveConversationToFinder {
                                    currentTab = HaqTab.FINDER
                                }
                            },
                            modifier = contentModifier
                        )

                        HaqTab.ADMIN -> AdminPortalScreen(
                            schemesState = schemesState,
                            checkLogsState = checkLogsState,
                            selectedLanguage = selectedLanguage,
                            onRetrySync = viewModel::retryFirestoreSync,
                            onAddSchemeClick = {
                                editingScheme = null
                                showSchemeEditor = true
                            },
                            onEditSchemeClick = { scheme ->
                                editingScheme = scheme
                                showSchemeEditor = true
                            },
                            onVerifyTodayClick = { scheme ->
                                viewModel.markSchemeVerifiedToday(scheme)
                            },
                            onToggleActiveClick = viewModel::toggleSchemeActive,
                            onDeleteSchemeClick = viewModel::deleteScheme,
                            modifier = contentModifier
                        )

                        HaqTab.FUNDING -> BusinessModelScreen(
                            partnersState = partnersState,
                            currentUserId = currentUserId,
                            selectedLanguage = selectedLanguage,
                            onAddPartner = viewModel::addPartnerFunding,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }

    if (showSchemeEditor) {
        SchemeEditorSheet(
            existingScheme = editingScheme,
            currentUserId = currentUserId,
            onSave = { existingId, scheme ->
                viewModel.saveScheme(existingId, scheme) {
                    showSchemeEditor = false
                    editingScheme = null
                }
            },
            onDismiss = {
                showSchemeEditor = false
                editingScheme = null
            }
        )
    }

    val activeChecklistProfile = activeProfile
    val activeChecklistMatches = checklistMatches
    if (activeChecklistProfile != null && !activeChecklistMatches.isNullOrEmpty()) {
        DocumentChecklistSheet(
            profile = activeChecklistProfile,
            matches = activeChecklistMatches,
            strings = strings,
            onChecklistExported = viewModel::recordChecklistDownloaded,
            onDismiss = { checklistMatches = null }
        )
    }
}
