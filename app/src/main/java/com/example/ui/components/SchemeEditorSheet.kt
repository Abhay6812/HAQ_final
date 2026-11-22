package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.EligibilityRuleEngine
import com.example.model.FarmerScheme
import com.example.model.SchemeCategory
import com.example.model.TargetGender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeEditorSheet(
    existingScheme: FarmerScheme?,
    currentUserId: String,
    onSave: (existingId: String?, scheme: FarmerScheme) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(existingScheme?.name ?: "") }
    var shortName by remember { mutableStateOf(existingScheme?.shortName ?: "") }
    var department by remember { mutableStateOf(existingScheme?.department ?: "State Agriculture Dept") }
    var category by remember { mutableStateOf(SchemeCategory.fromRaw(existingScheme?.category)) }
    var benefitSummary by remember { mutableStateOf(existingScheme?.benefitSummary ?: "") }
    var fundingSponsor by remember { mutableStateOf(existingScheme?.fundingSponsor ?: "State Agri Dept • KVK Verified") }
    var statesInput by remember { mutableStateOf(existingScheme?.eligibleStates?.joinToString(", ") ?: "Maharashtra") }
    var maxAcresInput by remember { mutableStateOf(existingScheme?.maxLandAcres?.toString() ?: "5.0") }
    var maxIncomeInput by remember { mutableStateOf(existingScheme?.maxAnnualIncomeInr?.toString() ?: "200000") }
    var targetGender by remember { mutableStateOf(TargetGender.fromRaw(existingScheme?.targetGender)) }
    var docsInput by remember {
        mutableStateOf(
            existingScheme?.requiredDocuments?.joinToString("\n")
                ?: "7/12 Land Extract (Satbara)\nAnnual Income Certificate"
        )
    }
    var sourceUrl by remember { mutableStateOf(existingScheme?.officialSourceUrl ?: "https://mahadbt.maharashtra.gov.in/") }
    var verifiedByOrg by remember { mutableStateOf(existingScheme?.verifiedByOrg ?: "KVK Official Desk") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existingScheme == null) "Add Editable Welfare Scheme" else "Edit Scheme Rules & Source",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Changes save directly to Cloud Firestore and immediately update the live Farmer Benefits Finder rule engine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Official Scheme Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("scheme_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("Short Label *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = statesInput,
                    onValueChange = { statesInput = it },
                    label = { Text("States (or ALL)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                label = { Text("Government Department *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SchemeCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = maxAcresInput,
                    onValueChange = { maxAcresInput = it },
                    label = { Text("Max Land (Acres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxIncomeInput,
                    onValueChange = { maxIncomeInput = it },
                    label = { Text("Max Income (₹/yr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Target Beneficiary Group",
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetGender.entries.forEach { g ->
                    FilterChip(
                        selected = targetGender == g,
                        onClick = { targetGender = g },
                        label = { Text(g.label) }
                    )
                }
            }

            OutlinedTextField(
                value = benefitSummary,
                onValueChange = { benefitSummary = it },
                label = { Text("Benefit Summary *") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = sourceUrl,
                onValueChange = { sourceUrl = it },
                label = { Text("Official Portal URL (https://) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = verifiedByOrg,
                    onValueChange = { verifiedByOrg = it },
                    label = { Text("Verified By (KVK/Dept)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fundingSponsor,
                    onValueChange = { fundingSponsor = it },
                    label = { Text("Funding Sponsor") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = docsInput,
                onValueChange = { docsInput = it },
                label = { Text("Required Documents (one per line)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val parsedAcres = maxAcresInput.toDoubleOrNull()
                        val parsedIncome = maxIncomeInput.toLongOrNull()
                        when {
                            name.isBlank() -> validationError = "Scheme name is required."
                            shortName.isBlank() -> validationError = "Short label is required."
                            benefitSummary.isBlank() -> validationError = "Benefit summary is required."
                            parsedAcres == null || parsedAcres < 0.0 -> validationError = "Enter a valid landholding limit in acres."
                            parsedIncome == null || parsedIncome < 0L -> validationError = "Enter a valid annual income threshold in INR."
                            !sourceUrl.trim().startsWith("https://") -> validationError = "Official URL must start with https://"
                            else -> {
                                validationError = null
                                val states = statesInput.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .ifEmpty { listOf("ALL") }
                                val docs = docsInput.split("\n")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .ifEmpty { listOf("7/12 Land Extract (Satbara)") }
                                val built = FarmerScheme(
                                    id = existingScheme?.id ?: "",
                                    userId = currentUserId,
                                    name = name.trim(),
                                    shortName = shortName.trim(),
                                    department = department.trim().ifBlank { "State Agriculture Dept" },
                                    category = category.name,
                                    benefitSummary = benefitSummary.trim(),
                                    fundingSponsor = fundingSponsor.trim().ifBlank { "State Agri Dept" },
                                    eligibleStates = states,
                                    maxLandAcres = parsedAcres,
                                    maxAnnualIncomeInr = parsedIncome,
                                    targetGender = targetGender.name,
                                    requiredDocuments = docs,
                                    officialSourceUrl = sourceUrl.trim(),
                                    lastVerifiedDate = EligibilityRuleEngine.todayFormattedDate(),
                                    verifiedByOrg = verifiedByOrg.trim().ifBlank { "KVK Official Desk" },
                                    active = existingScheme?.active ?: true
                                )
                                onSave(existingScheme?.id, built)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_scheme_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save Live Rule")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
