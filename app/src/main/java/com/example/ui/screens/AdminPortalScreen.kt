package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.EligibilityCheckLog
import com.example.model.EligibilityRuleEngine
import com.example.model.FarmerScheme
import com.example.model.HaqLocalization
import com.example.model.HaqStrings
import com.example.model.SchemeCategory
import com.example.model.TargetGender
import com.example.ui.theme.ForestDeep
import com.example.ui.theme.MonoLabelStyle
import com.example.ui.theme.MonoMetricStyle
import com.example.viewmodel.UiState

@Composable
fun AdminPortalScreen(
    schemesState: UiState<List<FarmerScheme>>,
    checkLogsState: UiState<List<EligibilityCheckLog>>,
    selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    onRetrySync: () -> Unit = {},
    onAddSchemeClick: () -> Unit,
    onEditSchemeClick: (FarmerScheme) -> Unit,
    onVerifyTodayClick: (FarmerScheme) -> Unit,
    onToggleActiveClick: (FarmerScheme) -> Unit,
    onDeleteSchemeClick: (FarmerScheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = remember(selectedLanguage) { HaqLocalization.strings(selectedLanguage) }
    var schemeToDelete by remember { mutableStateOf<FarmerScheme?>(null) }

    val schemes = (schemesState as? UiState.Success)?.data.orEmpty()
    val logs = (checkLogsState as? UiState.Success)?.data.orEmpty()

    val activeSchemesCount = schemes.count { it.active }
    val checksCompleted = logs.size
    val totalMatchesGenerated = logs.sumOf { it.matchedCount }
    val checklistsExported = logs.count { it.checklistDownloaded }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // B2B/B2G Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_kvk_partner_1795319112581),
                    contentDescription = "Krishi Vigyan Kendra agricultural advisory center",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ForestDeep.copy(alpha = 0.35f),
                                    ForestDeep.copy(alpha = 0.90f)
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
                        color = Color(0xFF74C69D).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "B2G • CSR • KVK OFFICIAL ADMIN VIEW",
                            style = MonoLabelStyle,
                            color = Color(0xFFD8F3DC),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.adminHeroTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = strings.adminHeroSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Live Impact Telemetry Grid (Zero-PII)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.impactMetricsTitle,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ImpactMetricTile(
                        label = strings.metricFarmersAssisted,
                        value = "$checksCompleted",
                        footnote = "Live queries logged",
                        modifier = Modifier.weight(1f)
                    )
                    ImpactMetricTile(
                        label = strings.metricMatchesFound,
                        value = "$totalMatchesGenerated",
                        footnote = "Likely eligible hits",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ImpactMetricTile(
                        label = strings.metricChecklistsSaved,
                        value = "$checklistsExported",
                        footnote = "Actionable takeaways",
                        modifier = Modifier.weight(1f)
                    )
                    ImpactMetricTile(
                        label = strings.metricActiveRules,
                        value = "$activeSchemesCount / ${schemes.size}",
                        footnote = "Synced in Firestore",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Scheme Rules Store Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.editableRulesHeader,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = strings.editableRulesSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddSchemeClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_add_scheme_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = strings.addSchemeButton,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addSchemeButton)
                }
            }
        }

        when (schemesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
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
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = schemesState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            OutlinedButton(
                                onClick = onRetrySync,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("admin_retry_sync_button")
                            ) {
                                Text("Retry Sync")
                            }
                        }
                    }
                }
            }
            is UiState.Success -> {
                items(
                    items = schemes,
                    key = { it.id.ifBlank { it.shortName } }
                ) { scheme ->
                    AdminSchemeCard(
                        scheme = scheme,
                        strings = strings,
                        onEdit = { onEditSchemeClick(scheme) },
                        onVerifyToday = { onVerifyTodayClick(scheme) },
                        onToggleActive = { onToggleActiveClick(scheme) },
                        onDelete = { schemeToDelete = scheme }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (schemeToDelete != null) {
        val target = schemeToDelete!!
        AlertDialog(
            onDismissRequest = { schemeToDelete = null },
            title = { Text("Remove '${target.shortName}'?") },
            text = {
                Text("This will delete the scheme from your live editable Firestore store. You can always add it again later.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSchemeClick(target)
                        schemeToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { schemeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ImpactMetricTile(
    label: String,
    value: String,
    footnote: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MonoMetricStyle,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = footnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminSchemeCard(
    scheme: FarmerScheme,
    strings: HaqStrings,
    onEdit: () -> Unit,
    onVerifyToday: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val landCapLabel = if (scheme.maxLandAcres >= 500.0) {
        "Any land"
    } else {
        "≤ ${EligibilityRuleEngine.formatAcres(scheme.maxLandAcres)} ac"
    }
    val incomeCapLabel = if (scheme.maxAnnualIncomeInr >= 50_000_000L) {
        "No income cap"
    } else {
        "≤ ${EligibilityRuleEngine.formatRupees(scheme.maxAnnualIncomeInr)}"
    }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scheme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${SchemeCategory.fromRaw(scheme.category).label} • ${scheme.eligibleStates.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = scheme.active,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("toggle_scheme_${scheme.shortName.lowercase().replace(" ", "_")}")
                )
            }

            // Live Rule Parameters Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuleChip("Land: $landCapLabel")
                RuleChip("Income: $incomeCapLabel")
                RuleChip(TargetGender.fromRaw(scheme.targetGender).label)
            }

            // Verification Metadata
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${strings.verifiedPrefix} ${scheme.lastVerifiedDate} • ${scheme.verifiedByOrg}",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Admin Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onVerifyToday,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = strings.verifyTodayButton,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(strings.verifyTodayButton, style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = strings.editRulesButton,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(strings.editRulesButton, style = MaterialTheme.typography.labelMedium)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete scheme",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MonoLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
