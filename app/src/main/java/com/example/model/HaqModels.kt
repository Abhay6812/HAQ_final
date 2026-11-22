package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SchemeCategory(val label: String) {
    INCOME_SUPPORT("Income Support"),
    EQUIPMENT_SUBSIDY("Equipment Subsidy"),
    CROP_INSURANCE("Crop Insurance"),
    SOLAR_IRRIGATION("Solar & Irrigation"),
    CREDIT_LOAN("Low-Interest Credit"),
    WOMEN_FARMER("Women Farmer Priority");

    companion object {
        fun fromRaw(raw: String?): SchemeCategory =
            entries.firstOrNull { it.name == raw } ?: INCOME_SUPPORT
    }
}

enum class TargetGender(val label: String) {
    ALL("All Farmers"),
    FEMALE("Female Farmers"),
    MALE("Male Farmers");

    companion object {
        fun fromRaw(raw: String?): TargetGender =
            entries.firstOrNull { it.name == raw } ?: ALL
    }
}

enum class PartnerType(val label: String, val roleSummary: String) {
    STATE_AGRI_DEPT(
        "State Agriculture Dept",
        "Funds rural scheme digitization & KVK outreach portals"
    ),
    CSR_FUND(
        "CSR Agri-Impact Fund",
        "Sponsors zero-fee farmer access & regional dialect AI support"
    ),
    SEED_FERTILIZER_COOP(
        "Seed & Fertilizer Co-op",
        "Underwrites verified scheme discovery at village co-op desks"
    ),
    KVK_CENTER(
        "Krishi Vigyan Kendra (KVK)",
        "Verifies rule updates, dates & local document checklists"
    );

    companion object {
        fun fromRaw(raw: String?): PartnerType =
            entries.firstOrNull { it.name == raw } ?: CSR_FUND
    }
}

enum class InputMode(val label: String) {
    PLAIN_LANGUAGE("Plain Language"),
    VOICE_DIALECT("Voice / Dialect"),
    DEMO_PROFILE("Quick Demo");

    companion object {
        fun fromRaw(raw: String?): InputMode =
            entries.firstOrNull { it.name == raw } ?: PLAIN_LANGUAGE
    }
}

/**
 * Guardrail #1: Never frame results as "Guaranteed". Always frame as "Likely Eligible".
 */
enum class EligibilityStatus(val label: String) {
    LIKELY_ELIGIBLE("Likely Eligible"),
    PARTIAL_VERIFY("Partially Likely • Verify Locally")
}

@Keep
@IgnoreExtraProperties
data class FarmerScheme(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val shortName: String = "",
    val department: String = "",
    val category: String = SchemeCategory.INCOME_SUPPORT.name,
    val benefitSummary: String = "",
    val fundingSponsor: String = "",
    val eligibleStates: List<String> = listOf("ALL"),
    val maxLandAcres: Double = 999.0,
    val maxAnnualIncomeInr: Long = 99999999L,
    val targetGender: String = TargetGender.ALL.name,
    val requiredDocuments: List<String> = emptyList(),
    val officialSourceUrl: String = "https://pmkisan.gov.in/",
    val lastVerifiedDate: String = "21 Nov 2026",
    val verifiedByOrg: String = "KVK Verification Desk",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EligibilityCheckLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val stateRegion: String = "Maharashtra",
    val inputMode: String = InputMode.PLAIN_LANGUAGE.name,
    val matchedCount: Long = 0L,
    val checklistDownloaded: Boolean = false,
    val partnerChannel: String = "KVK Field Desk",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PartnerFunding(
    @DocumentId val id: String = "",
    val userId: String = "",
    val organizationName: String = "",
    val partnerType: String = PartnerType.CSR_FUND.name,
    val regionCovered: String = "",
    val farmersSponsoredTarget: Long = 0L,
    val fundingContributionLabel: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Strictly zero-PII parsed farmer query. Never holds Aadhaar, passbook, or bank details.
 */
data class FarmerProfileQuery(
    val rawPlainText: String,
    val detectedLanguage: String = "English",
    val state: String = "Maharashtra",
    val landAcres: Double = 2.0,
    val annualIncomeInr: Long = 150000L,
    val gender: TargetGender = TargetGender.ALL,
    val primaryNeed: String = "GENERAL",
    val aiUnderstandingNote: String = "",
    val inputMode: InputMode = InputMode.PLAIN_LANGUAGE
)

data class RuleEvaluationItem(
    val ruleNumber: Int,
    val title: String,
    val detail: String,
    val passed: Boolean
)

data class SchemeMatchResult(
    val scheme: FarmerScheme,
    val status: EligibilityStatus,
    val matchedRules: List<RuleEvaluationItem>,
    val matchScorePercent: Int
)

data class JudgeDemoProfile(
    val id: String,
    val title: String,
    val subtitle: String,
    val sampleDialectInput: String,
    val profileQuery: FarmerProfileQuery
)

object PreSeededDemos {
    val profiles: List<JudgeDemoProfile> = listOf(
        JudgeDemoProfile(
            id = "demo_maharashtra_small",
            title = "Demo: Small Farmer in Maharashtra (Family Income < ₹1.5L)",
            subtitle = "1.8 acres • Nashik, Maharashtra • Income ₹1.2L • Needs income support & crop cover",
            sampleDialectInput = "मी महाराष्ट्रातील लहान शेतकरी आहे. माझ्याकडे १.८ एकर शेती आहे आणि वार्षिक उत्पन्न ₹१,२०,००० आहे. मला शेती मदत आणि पीक विमा योजना हवी आहे.",
            profileQuery = FarmerProfileQuery(
                rawPlainText = "मी महाराष्ट्रातील लहान शेतकरी आहे. माझ्याकडे १.८ एकर शेती आहे आणि वार्षिक उत्पन्न ₹१,२०,००० आहे. मला शेती मदत आणि पीक विमा योजना हवी आहे.",
                detectedLanguage = "Marathi",
                state = "Maharashtra",
                landAcres = 1.8,
                annualIncomeInr = 120000L,
                gender = TargetGender.ALL,
                primaryNeed = SchemeCategory.INCOME_SUPPORT.name,
                aiUnderstandingNote = "Understood from Marathi input: Small landholding farmer in Maharashtra (1.8 acres, ₹1.2L annual income) seeking direct income support & crop protection.",
                inputMode = InputMode.DEMO_PROFILE
            )
        ),
        JudgeDemoProfile(
            id = "demo_female_equipment",
            title = "Demo: Female Farmer seeking Equipment Subsidy",
            subtitle = "2.5 acres • Female Farmer • Income ₹1.8L • Seeking power tiller / tractor subsidy",
            sampleDialectInput = "मैं महाराष्ट्र की महिला किसान हूँ। मेरे पास 2.5 एकड़ ज़मीन है, परिवार की सालाना आय ₹1.8 लाख है और मुझे कृषि यंत्र / पावर टिलर सब्सिडी चाहिए।",
            profileQuery = FarmerProfileQuery(
                rawPlainText = "मैं महाराष्ट्र की महिला किसान हूँ। मेरे पास 2.5 एकड़ ज़मीन है, परिवार की सालाना आय ₹1.8 लाख है और मुझे कृषि यंत्र / पावर टिलर सब्सिडी चाहिए।",
                detectedLanguage = "Hindi",
                state = "Maharashtra",
                landAcres = 2.5,
                annualIncomeInr = 180000L,
                gender = TargetGender.FEMALE,
                primaryNeed = SchemeCategory.EQUIPMENT_SUBSIDY.name,
                aiUnderstandingNote = "Understood from Hindi input: Woman farmer in Maharashtra (2.5 acres, ₹1.8L income) seeking subsidized farm equipment & women-priority mechanization support.",
                inputMode = InputMode.DEMO_PROFILE
            )
        )
    )
}
