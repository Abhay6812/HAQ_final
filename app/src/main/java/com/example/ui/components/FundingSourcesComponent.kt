package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.HaqLocalization
import com.example.model.HaqStrings
import com.example.model.PartnerFunding
import com.example.model.PartnerType
import com.example.ui.theme.DarkEmeraldPrimary
import com.example.ui.theme.DarkGoldSecondary
import com.example.ui.theme.DarkTerracotta
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.MonoLabelStyle
import com.example.ui.theme.TerracottaWarm
import androidx.compose.foundation.isSystemInDarkTheme

private data class FundingPillarSpec(
    val id: String,
    val title: String,
    val shortLabel: String,
    val sharePercent: Int,
    val accentColor: Color,
    val icon: ImageVector,
    val whoPays: String,
    val whatItFunds: String,
    val whyTheyPay: String,
    val matchingTypes: Set<PartnerType>
)

private val fundingPillars = listOf(
    FundingPillarSpec(
        id = "pillar_state_agri",
        title = "State Agriculture Departments (B2G)",
        shortLabel = "State Agri Depts",
        sharePercent = 45,
        accentColor = ForestEmerald,
        icon = Icons.Filled.AccountBalance,
        whoPays = "State Departments of Agriculture, MahaDBT / district digital missions, and Krishi Vigyan Kendras (KVKs).",
        whatItFunds = "Live editable scheme rules store, monthly rule verification audits, and CSC / KVK field desk deployments.",
        whyTheyPay = "Increases last-mile welfare utilization, cuts rejected applications via pre-verified document checklists, and gives real-time zero-PII district demand metrics.",
        matchingTypes = setOf(PartnerType.STATE_AGRI_DEPT, PartnerType.KVK_CENTER)
    ),
    FundingPillarSpec(
        id = "pillar_csr_funds",
        title = "CSR Agri-Impact Funds",
        shortLabel = "CSR Funds",
        sharePercent = 35,
        accentColor = HarvestGold,
        icon = Icons.Filled.VolunteerActivism,
        whoPays = "Rural banking foundations, agribusiness CSR arms, and ESG impact grants under India's Section 135 CSR mandate.",
        whatItFunds = "Regional dialect voice & plain-language AI parsing, women farmer outreach, and ₹0 free farmer access.",
        whyTheyPay = "Delivers auditable rural livelihood impact (farmers matched & checklists exported) without collecting sensitive farmer PII.",
        matchingTypes = setOf(PartnerType.CSR_FUND)
    ),
    FundingPillarSpec(
        id = "pillar_seed_coops",
        title = "Seed & Fertilizer Co-ops (FPOs)",
        shortLabel = "Seed/Fertilizer Co-ops",
        sharePercent = 20,
        accentColor = TerracottaWarm,
        icon = Icons.Filled.Groups,
        whoPays = "Farmer Producer Organizations (FPOs), village cooperative societies, and agri-input co-op federations.",
        whatItFunds = "Co-op desk benefit discovery kiosks and seasonal crop-insurance (PMFBY) & credit (KCC) enrollment drives.",
        whyTheyPay = "Improves member liquidity and resilience when farmers claim subsidies on time, strengthening loyal cooperative trade with zero middlemen.",
        matchingTypes = setOf(PartnerType.SEED_FERTILIZER_COOP)
    )
)

/**
 * Full transparent 'Funding Sources' component for judges & partners.
 * Explicitly demonstrates who pays (CSR funds, State Agriculture Depts, Seed/Fertilizer Co-ops)
 * so farmers never pay a single rupee.
 */
@Composable
fun FundingSourcesSection(
    partners: List<PartnerFunding>,
    strings: HaqStrings = HaqLocalization.strings(AppLanguage.ENGLISH),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var expandedPillarId by remember { mutableStateOf<String?>(fundingPillars.first().id) }

    // Compute live share proportions from active Firestore sponsors if available; fall back to model targets
    val pillarMetrics = remember(partners) {
        val activePartners = partners.filter { it.active }
        val totalReach = activePartners.sumOf { it.farmersSponsoredTarget }.coerceAtLeast(0L)
        fundingPillars.associateWith { spec ->
            val matched = activePartners.filter {
                PartnerType.fromRaw(it.partnerType) in spec.matchingTypes
            }
            val reach = matched.sumOf { it.farmersSponsoredTarget }
            val livePercent = if (totalReach > 0L && reach > 0L) {
                ((reach.toDouble() / totalReach.toDouble()) * 100).toInt().coerceIn(5, 90)
            } else {
                spec.sharePercent
            }
            Triple(matched.size, reach, livePercent)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("funding_sources_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header + Zero Fee Promise
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.fundingSectionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.fundingSectionSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = strings.zeroFarmerFeePill,
                    style = MonoLabelStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Proportional Revenue Mix Bar Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SPONSORSHIP MIX (LIVE REACH SHARE)",
                    style = MonoLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Segmented bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    fundingPillars.forEach { spec ->
                        val weight = (pillarMetrics[spec]?.third ?: spec.sharePercent).toFloat()
                        Box(
                            modifier = Modifier
                                .weight(weight.coerceAtLeast(1f))
                                .fillMaxHeight()
                                .background(spec.accentColor)
                        )
                    }
                }

                // Compact legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    fundingPillars.forEach { spec ->
                        val pct = pillarMetrics[spec]?.third ?: spec.sharePercent
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(spec.accentColor)
                            )
                            Text(
                                text = "${spec.shortLabel} $pct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Expandable Pillar Transparency Cards
        fundingPillars.forEach { spec ->
            val (partnerCount, sponsoredReach, sharePct) = pillarMetrics[spec]
                ?: Triple(0, 0L, spec.sharePercent)
            val isExpanded = expandedPillarId == spec.id
            val resolvedAccent = if (isDark) {
                when (spec.id) {
                    "pillar_state_agri" -> DarkEmeraldPrimary
                    "pillar_csr_funds" -> DarkGoldSecondary
                    else -> DarkTerracotta
                }
            } else {
                spec.accentColor
            }

            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = if (isExpanded) 3.dp else 1.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("funding_pillar_${spec.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedPillarId = if (isExpanded) null else spec.id
                        }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(resolvedAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.title,
                                tint = resolvedAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = spec.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val metaLine = if (partnerCount > 0) {
                                "$partnerCount active sponsor(s) • %,d farmers • $sharePct%% share".format(sponsoredReach)
                            } else {
                                "Target share: $sharePct% • Institutional funding pillar"
                            }
                            Text(
                                text = metaLine,
                                style = MonoLabelStyle,
                                color = resolvedAccent
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse funding details" else "Expand funding details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            FundingDetailLine(label = "WHO PAYS", body = spec.whoPays)
                            FundingDetailLine(label = "WHAT IT FUNDS", body = spec.whatItFunds)
                            FundingDetailLine(label = "WHY THEY PAY", body = spec.whyTheyPay)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FundingDetailLine(label: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MonoLabelStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Compact scannable 'Funding Sources' strip for the Farmer Finder home screen
 * so judges immediately see who funds Haq without navigating away first.
 */
@Composable
fun FundingSourcesCompactCard(
    onExploreClick: () -> Unit,
    strings: HaqStrings = HaqLocalization.strings(AppLanguage.ENGLISH),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val stateAgriColor = if (isDark) DarkEmeraldPrimary else ForestEmerald
    val csrColor = if (isDark) DarkGoldSecondary else HarvestGold
    val coopColor = if (isDark) DarkTerracotta else TerracottaWarm

    Card(
        onClick = onExploreClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("finder_funding_sources_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.fundingCompactHeader,
                    style = MonoLabelStyle,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = strings.fundingCompactDetails,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View funding sources details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = strings.fundingCompactBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FundingPill("State Agri", stateAgriColor, Modifier.weight(1f))
                FundingPill("CSR Funds", csrColor, Modifier.weight(1f))
                FundingPill("Seed Co-ops", coopColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FundingPill(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1
            )
        }
    }
}
