package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.HaqLocalization
import com.example.model.PartnerFunding
import com.example.model.PartnerType
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MonoLabelStyle
import com.example.ui.theme.MonoMetricStyle
import com.example.viewmodel.UiState

@Composable
fun BusinessModelScreen(
    partnersState: UiState<List<PartnerFunding>>,
    currentUserId: String,
    selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    onAddPartner: (PartnerFunding, onDone: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = remember(selectedLanguage) { HaqLocalization.strings(selectedLanguage) }
    var showAddDialog by remember { mutableStateOf(false) }

    val partners = (partnersState as? UiState.Success)?.data.orEmpty()
    val totalFarmersSponsored = partners.filter { it.active }.sumOf { it.farmersSponsoredTarget }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Business Viability Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestEmerald),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color(0xFFF4A261).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "BUSINESS VIABILITY • ZERO FARMER FEES",
                            style = MonoLabelStyle,
                            color = Color(0xFFFFE5B4),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = strings.whoPaysHeroTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = strings.whoPaysHeroSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SPONSORED FARMER REACH",
                                    style = MonoLabelStyle,
                                    color = Color(0xFFD8F3DC)
                                )
                                Text(
                                    text = "%,d+ Farmers".format(totalFarmersSponsored),
                                    style = MonoMetricStyle,
                                    color = Color.White
                                )
                            }
                            Surface(
                                color = Color(0xFFF4A261),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${partners.size} Active Payers",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF112A20),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dedicated 'Funding Sources' Transparency Component (CSR, State Agri Depts, Seed/Fertilizer Co-ops)
        item {
            com.example.ui.components.FundingSourcesSection(
                partners = partners,
                strings = strings
            )
        }

        // Live Institutional Sponsors from Firestore
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.activeSponsorsHeader,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_partner_sponsor_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = strings.addSponsorButton,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.addSponsorButton)
                }
            }
        }

        items(
            items = partners,
            key = { it.id.ifBlank { it.organizationName } }
        ) { partner ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = partner.organizationName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = PartnerType.fromRaw(partner.partnerType).label,
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = partner.fundingContributionLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Coverage: ${partner.regionCovered} • Target: %,d farmers".format(partner.farmersSponsoredTarget),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // PS5 Guardrails Compliance Audit Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Compliance shield",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Public Welfare Guardrails Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    ComplianceRow("Results framed strictly as 'Likely Eligible' (never 'Guaranteed')")
                    ComplianceRow("Zero-PII architecture: No Aadhaar, land passbooks, or bank details stored")
                    ComplianceRow("Live editable Cloud Firestore rules store with Last-Verified dates & official HTTPS portals")
                    ComplianceRow("Plain-language & regional dialect voice/text input + 1-click document checklists")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showAddDialog) {
        AddPartnerSponsorDialog(
            currentUserId = currentUserId,
            onSave = { partner ->
                onAddPartner(partner) { showAddDialog = false }
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun ComplianceRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun AddPartnerSponsorDialog(
    currentUserId: String,
    onSave: (PartnerFunding) -> Unit,
    onDismiss: () -> Unit
) {
    var orgName by remember { mutableStateOf("") }
    var partnerType by remember { mutableStateOf(PartnerType.CSR_FUND) }
    var region by remember { mutableStateOf("Pune & Ahmednagar Districts") }
    var farmersTarget by remember { mutableStateOf("50000") }
    var contribution by remember { mutableStateOf("₹10L CSR Rural Welfare Grant • ₹0 Farmer Fee") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Institutional Sponsor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = orgName,
                    onValueChange = { orgName = it },
                    label = { Text("Organization Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PartnerType.entries.forEach { type ->
                        FilterChip(
                            selected = partnerType == type,
                            onClick = { partnerType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Region Covered") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = farmersTarget,
                    onValueChange = { farmersTarget = it },
                    label = { Text("Farmers Sponsored Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contribution,
                    onValueChange = { contribution = it },
                    label = { Text("Funding Model Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (orgName.isNotBlank()) {
                        onSave(
                            PartnerFunding(
                                userId = currentUserId,
                                organizationName = orgName.trim(),
                                partnerType = partnerType.name,
                                regionCovered = region.trim().ifBlank { "India" },
                                farmersSponsoredTarget = farmersTarget.toLongOrNull() ?: 25000L,
                                fundingContributionLabel = contribution.trim().ifBlank { "Zero Farmer Fee Sponsor" },
                                active = true
                            )
                        )
                    }
                },
                enabled = orgName.isNotBlank()
            ) {
                Text("Save Sponsor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
