package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EligibilityRuleEngine {

    fun evaluateAll(
        schemes: List<FarmerScheme>,
        profile: FarmerProfileQuery,
        language: AppLanguage = AppLanguage.ENGLISH
    ): List<SchemeMatchResult> {
        return schemes
            .filter { it.active }
            .mapNotNull { scheme -> evaluateScheme(scheme, profile, language) }
            .sortedWith(
                compareByDescending<SchemeMatchResult> { it.status == EligibilityStatus.LIKELY_ELIGIBLE }
                    .thenByDescending { it.matchScorePercent }
                    .thenBy { it.scheme.shortName }
            )
    }

    private fun evaluateScheme(
        scheme: FarmerScheme,
        profile: FarmerProfileQuery,
        language: AppLanguage
    ): SchemeMatchResult? {
        val strings = HaqLocalization.strings(language)
        val rules = mutableListOf<RuleEvaluationItem>()
        var ruleIdx = 1

        // Rule 1: Landholding Threshold
        val noLandCap = scheme.maxLandAcres >= 500.0
        val landPassed = noLandCap || profile.landAcres <= scheme.maxLandAcres
        val landLabel = if (noLandCap) {
            when (language) {
                AppLanguage.HINDI -> "भूमि धारणा: कोई भी कृषि भूमि (${formatAcres(profile.landAcres)} एकड़)"
                AppLanguage.MARATHI -> "जमीन धारणा: कोणतीही लागवडयोग्य जमीन (${formatAcres(profile.landAcres)} एकर)"
                AppLanguage.ENGLISH -> "Landholding: Any cultivable land (${formatAcres(profile.landAcres)} acres)"
            }
        } else {
            when (language) {
                AppLanguage.HINDI -> "भूमि ${formatAcres(profile.landAcres)} एकड़ ≤ ${formatAcres(scheme.maxLandAcres)} एकड़ सीमा"
                AppLanguage.MARATHI -> "जमीन ${formatAcres(profile.landAcres)} एकर ≤ ${formatAcres(scheme.maxLandAcres)} एकर मर्यादा"
                AppLanguage.ENGLISH -> "Landholding ${formatAcres(profile.landAcres)} Acres ≤ ${formatAcres(scheme.maxLandAcres)} Acres Limit"
            }
        }
        rules.add(
            RuleEvaluationItem(
                ruleNumber = ruleIdx++,
                title = strings.rule1Title,
                detail = if (landPassed) landLabel else "Profile (${formatAcres(profile.landAcres)} acres) exceeds ${formatAcres(scheme.maxLandAcres)} acre cap",
                passed = landPassed
            )
        )

        // Rule 2: Annual Income Threshold
        val noIncomeCap = scheme.maxAnnualIncomeInr >= 50_000_000L
        val incomePassed = noIncomeCap || profile.annualIncomeInr <= scheme.maxAnnualIncomeInr
        val incomeLabel = if (noIncomeCap) {
            when (language) {
                AppLanguage.HINDI -> "वार्षिक आय: सभी गैर-करदाता किसान परिवारों के लिए (${formatRupees(profile.annualIncomeInr)})"
                AppLanguage.MARATHI -> "वार्षिक उत्पन्न: सर्व करमुक्त शेतकरी कुटुंबांसाठी (${formatRupees(profile.annualIncomeInr)})"
                AppLanguage.ENGLISH -> "Annual Income: Open to all non-taxpayer farm households (${formatRupees(profile.annualIncomeInr)})"
            }
        } else {
            when (language) {
                AppLanguage.HINDI -> "वार्षिक आय सीमा के भीतर (${formatRupees(profile.annualIncomeInr)} ≤ ${formatRupees(scheme.maxAnnualIncomeInr)})"
                AppLanguage.MARATHI -> "वार्षिक उत्पन्न मर्यादेत (${formatRupees(profile.annualIncomeInr)} ≤ ${formatRupees(scheme.maxAnnualIncomeInr)})"
                AppLanguage.ENGLISH -> "Annual Income under Threshold (${formatRupees(profile.annualIncomeInr)} ≤ ${formatRupees(scheme.maxAnnualIncomeInr)})"
            }
        }
        rules.add(
            RuleEvaluationItem(
                ruleNumber = ruleIdx++,
                title = strings.rule2Title,
                detail = if (incomePassed) incomeLabel else "Household income (${formatRupees(profile.annualIncomeInr)}) above ${formatRupees(scheme.maxAnnualIncomeInr)} threshold",
                passed = incomePassed
            )
        )

        // Rule 3: State Jurisdiction
        val isPanIndia = scheme.eligibleStates.any { it.equals("ALL", ignoreCase = true) }
        val statePassed = isPanIndia || scheme.eligibleStates.any {
            it.equals(profile.state, ignoreCase = true)
        }
        rules.add(
            RuleEvaluationItem(
                ruleNumber = ruleIdx++,
                title = strings.rule3Title,
                detail = when {
                    isPanIndia -> when (language) {
                        AppLanguage.HINDI -> "अखिल भारतीय केंद्रीय योजना (${profile.state} शामिल)"
                        AppLanguage.MARATHI -> "संपूर्ण भारत केंद्रीय योजना (${profile.state} समाविष्ट)"
                        AppLanguage.ENGLISH -> "Pan-India Central Scheme (Covers ${profile.state})"
                    }
                    statePassed -> when (language) {
                        AppLanguage.HINDI -> "${profile.state} राज्य में सक्रिय (${scheme.eligibleStates.joinToString(", ")})"
                        AppLanguage.MARATHI -> "${profile.state} राज्यात सक्रिय (${scheme.eligibleStates.joinToString(", ")})"
                        AppLanguage.ENGLISH -> "Active in ${profile.state} (${scheme.eligibleStates.joinToString(", ")})"
                    }
                    else -> "Limited to ${scheme.eligibleStates.joinToString(", ")}"
                },
                passed = statePassed
            )
        )

        // Rule 4: Beneficiary Gender Priority
        val targetGender = TargetGender.fromRaw(scheme.targetGender)
        val genderPassed = targetGender == TargetGender.ALL ||
            profile.gender == targetGender ||
            (profile.gender == TargetGender.ALL && targetGender == TargetGender.FEMALE && scheme.category == SchemeCategory.EQUIPMENT_SUBSIDY.name)
        val genderStrictPassed = targetGender == TargetGender.ALL || profile.gender == targetGender
        rules.add(
            RuleEvaluationItem(
                ruleNumber = ruleIdx++,
                title = strings.rule4Title,
                detail = when {
                    targetGender == TargetGender.FEMALE && profile.gender == TargetGender.FEMALE ->
                        when (language) {
                            AppLanguage.HINDI -> "महिला किसान प्राथमिकता पात्र (अतिरिक्त सब्सिडी वर्ग)"
                            AppLanguage.MARATHI -> "महिला शेतकरी प्राधान्य पात्र (अतिरिक्त अनुदान श्रेणी)"
                            AppLanguage.ENGLISH -> "Women Farmer Priority Qualified (Enhanced Subsidy Tier)"
                        }
                    targetGender == TargetGender.ALL ->
                        when (language) {
                            AppLanguage.HINDI -> "सभी पात्र किसानों के लिए खुला"
                            AppLanguage.MARATHI -> "सर्व पात्र शेतकऱ्यांसाठी खुले"
                            AppLanguage.ENGLISH -> "Open to All Eligible Farmers"
                        }
                    else ->
                        "Reserved for ${targetGender.label}"
                },
                passed = genderStrictPassed
            )
        )

        // Filter out hard non-matches (wrong state or strictly wrong gender)
        if (!statePassed) return null
        if (!genderStrictPassed && !genderPassed) return null

        // Rule 5: Need / Category Alignment bonus
        val needMatches = profile.primaryNeed == "GENERAL" ||
            scheme.category.equals(profile.primaryNeed, ignoreCase = true) ||
            (profile.gender == TargetGender.FEMALE && scheme.category == SchemeCategory.WOMEN_FARMER.name)

        if (needMatches && profile.primaryNeed != "GENERAL") {
            val catLabel = SchemeCategory.fromRaw(scheme.category).label
            rules.add(
                RuleEvaluationItem(
                    ruleNumber = ruleIdx,
                    title = strings.rule5Title,
                    detail = when (language) {
                        AppLanguage.HINDI -> "आपकी '$catLabel' आवश्यकता से सीधा मेल"
                        AppLanguage.MARATHI -> "तुमच्या '$catLabel' गरजेशी थेट जुळणी"
                        AppLanguage.ENGLISH -> "Directly addresses requested $catLabel support"
                    },
                    passed = true
                )
            )
        }

        val passedCount = rules.count { it.passed }
        val coreAllPassed = landPassed && incomePassed && statePassed && genderStrictPassed
        val rawPercent = ((passedCount.toDouble() / rules.size.toDouble()) * 100).toInt()
        val boostedPercent = if (coreAllPassed && needMatches) 98 else rawPercent.coerceIn(60, 94)

        val status = if (coreAllPassed) {
            EligibilityStatus.LIKELY_ELIGIBLE
        } else if (passedCount >= 3) {
            EligibilityStatus.PARTIAL_VERIFY
        } else {
            return null
        }

        return SchemeMatchResult(
            scheme = scheme,
            status = status,
            matchedRules = rules,
            matchScorePercent = boostedPercent
        )
    }

    fun formatRupees(amount: Long): String {
        return when {
            amount >= 100_000L -> {
                val lakhs = amount / 100_000.0
                if (lakhs % 1.0 == 0.0) "₹${lakhs.toInt()}L" else String.format(Locale.US, "₹%.2fL", lakhs)
            }
            amount >= 1_000L -> "₹${amount / 1_000}K"
            else -> "₹$amount"
        }
    }

    fun formatAcres(acres: Double): String {
        return if (acres % 1.0 == 0.0) "${acres.toInt()}" else String.format(Locale.US, "%.1f", acres)
    }

    fun todayFormattedDate(): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
    }
}

object ChecklistFormatter {

    fun buildDownloadableChecklist(
        profile: FarmerProfileQuery,
        matches: List<SchemeMatchResult>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("====================================================")
        sb.appendLine("HAQ — FARMER BENEFITS DOCUMENT CHECKLIST")
        sb.appendLine("Claim What's Yours • 100% Free for Farmers")
        sb.appendLine("====================================================")
        sb.appendLine("Generated On: ${EligibilityRuleEngine.todayFormattedDate()}")
        sb.appendLine("Region: ${profile.state} | Land: ${EligibilityRuleEngine.formatAcres(profile.landAcres)} Acres | Income: ${EligibilityRuleEngine.formatRupees(profile.annualIncomeInr)}")
        sb.appendLine()
        sb.appendLine("PRIVACY NOTICE (ZERO-PII GUARDRAIL):")
        sb.appendLine("Haq NEVER collects or stores Aadhaar numbers, 7/12 passbook numbers, or bank account details. Carry original documents ONLY to your official CSC, KVK, or State Agriculture Portal.")
        sb.appendLine()

        val allDocs = matches
            .flatMap { it.scheme.requiredDocuments }
            .distinct()

        sb.appendLine("MASTER DOCUMENT CHECKLIST TO CARRY (${allDocs.size} Items):")
        allDocs.forEachIndexed { index, doc ->
            sb.appendLine("  [ ] ${index + 1}. $doc")
        }
        sb.appendLine()
        sb.appendLine("LIKELY ELIGIBLE SCHEMES (${matches.size}):")
        sb.appendLine("----------------------------------------------------")
        matches.forEachIndexed { idx, match ->
            sb.appendLine("${idx + 1}. ${match.scheme.name} [${match.status.label}]")
            sb.appendLine("   Benefit: ${match.scheme.benefitSummary}")
            sb.appendLine("   Official Portal: ${match.scheme.officialSourceUrl}")
            sb.appendLine("   Last Verified: ${match.scheme.lastVerifiedDate} (${match.scheme.verifiedByOrg})")
            sb.appendLine("   Transparent Rule Match:")
            match.matchedRules.filter { it.passed }.forEach { rule ->
                sb.appendLine("     ✓ ${rule.title}: ${rule.detail}")
            }
            sb.appendLine("   Documents Needed:")
            match.scheme.requiredDocuments.forEach { doc ->
                sb.appendLine("     • $doc")
            }
            sb.appendLine()
        }
        sb.appendLine("----------------------------------------------------")
        sb.appendLine("LEGAL DISCLAIMER: All matches are estimates ('Likely Eligible'), never guaranteed. Final verification & disbursal are handled solely by the official government department.")
        return sb.toString()
    }
}
