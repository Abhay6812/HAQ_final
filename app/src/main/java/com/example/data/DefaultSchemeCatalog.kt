package com.example.data

import com.example.model.FarmerScheme
import com.example.model.PartnerFunding
import com.example.model.PartnerType
import com.example.model.SchemeCategory
import com.example.model.TargetGender

object DefaultSchemeCatalog {

    fun initialSchemes(userId: String): List<Pair<String, FarmerScheme>> = listOf(
        "pm_kisan" to FarmerScheme(
            userId = userId,
            name = "PM-KISAN Samman Nidhi",
            shortName = "PM-KISAN",
            department = "Ministry of Agriculture & Farmers Welfare",
            category = SchemeCategory.INCOME_SUPPORT.name,
            benefitSummary = "₹6,000/year direct income support in 3 equal ₹2,000 installments for cultivable landholding families.",
            fundingSponsor = "Central Govt • Verified via KVK Network",
            eligibleStates = listOf("ALL"),
            maxLandAcres = 5.0,
            maxAnnualIncomeInr = 250000L,
            targetGender = TargetGender.ALL.name,
            requiredDocuments = listOf(
                "7/12 Land Extract (Satbara) or Record of Rights",
                "Income Certificate / Self-Declaration",
                "Identity proof (Carry to CSC/Portal only — never entered in Haq)",
                "Active DBT-linked Bank Passbook (Carry to CSC only)"
            ),
            officialSourceUrl = "https://pmkisan.gov.in/",
            lastVerifiedDate = "21 Nov 2026",
            verifiedByOrg = "KVK Baramati Desk",
            active = true
        ),
        "namo_shetkari_mh" to FarmerScheme(
            userId = userId,
            name = "Namo Shetkari Mahasanman Nidhi Yojana",
            shortName = "Namo Shetkari",
            department = "Dept. of Agriculture, Govt. of Maharashtra",
            category = SchemeCategory.INCOME_SUPPORT.name,
            benefitSummary = "Additional ₹6,000/year state top-up over PM-KISAN (total ₹12,000/year combined support) for Maharashtra farmers.",
            fundingSponsor = "State Agriculture Dept, Maharashtra",
            eligibleStates = listOf("Maharashtra"),
            maxLandAcres = 5.0,
            maxAnnualIncomeInr = 200000L,
            targetGender = TargetGender.ALL.name,
            requiredDocuments = listOf(
                "7/12 Land Extract (Satbara) & 8A Khata Extract",
                "Maharashtra Domicile / Residence Proof",
                "PM-KISAN Registration Reference (Carry to MahaDBT)"
            ),
            officialSourceUrl = "https://nsmny.mahait.org/",
            lastVerifiedDate = "21 Nov 2026",
            verifiedByOrg = "Maharashtra State Agri Portal",
            active = true
        ),
        "smam_equipment_subsidy" to FarmerScheme(
            userId = userId,
            name = "SMAM Farm Mechanization & Equipment Subsidy",
            shortName = "SMAM Equipment",
            department = "Dept. of Agriculture & Cooperation (MahaDBT / Agrimachinery)",
            category = SchemeCategory.EQUIPMENT_SUBSIDY.name,
            benefitSummary = "40%–50% subsidy (50% priority for Women, Small & Marginal farmers) on power tillers, tractors, rotavators & sprayers.",
            fundingSponsor = "State Agri Dept & Seed/Equipment Co-op Alliance",
            eligibleStates = listOf("ALL"),
            maxLandAcres = 5.0,
            maxAnnualIncomeInr = 300000L,
            targetGender = TargetGender.ALL.name,
            requiredDocuments = listOf(
                "7/12 Land Extract & 8A Certificate",
                "Authorized Machinery Dealer Quotation",
                "Income Certificate (Tehsildar / CSC issued)",
                "Caste / Woman Farmer Priority Certificate (if applicable)"
            ),
            officialSourceUrl = "https://agrimachinery.nic.in/",
            lastVerifiedDate = "20 Nov 2026",
            verifiedByOrg = "KVK Nashik Mechanization Cell",
            active = true
        ),
        "mksp_women_farmer" to FarmerScheme(
            userId = userId,
            name = "Mahila Kisan Sashaktikaran Pariyojana (MKSP)",
            shortName = "MKSP Women",
            department = "Ministry of Rural Development • NRLM",
            category = SchemeCategory.WOMEN_FARMER.name,
            benefitSummary = "Dedicated women farmer equipment kits, drudgery-reduction tools, SHG tool-bank grants & sustainable farming training.",
            fundingSponsor = "Rural Livelihoods Mission & CSR Agri-Impact Fund",
            eligibleStates = listOf("ALL"),
            maxLandAcres = 5.0,
            maxAnnualIncomeInr = 250000L,
            targetGender = TargetGender.FEMALE.name,
            requiredDocuments = listOf(
                "Woman Farmer / SHG Membership Certificate",
                "7/12 Land Extract or Joint Family Cultivator Proof",
                "Annual Family Income Certificate"
            ),
            officialSourceUrl = "https://aajeevika.gov.in/",
            lastVerifiedDate = "21 Nov 2026",
            verifiedByOrg = "Sahyadri CSR Women Agri Desk",
            active = true
        ),
        "pmfby_crop_insurance" to FarmerScheme(
            userId = userId,
            name = "Pradhan Mantri Fasal Bima Yojana (PMFBY)",
            shortName = "PMFBY Insurance",
            department = "Ministry of Agriculture & Farmers Welfare",
            category = SchemeCategory.CROP_INSURANCE.name,
            benefitSummary = "Comprehensive crop loss cover against drought, hailstorm & pests at nominal 1.5% Rabi / 2% Kharif farmer premium (₹1 in Maharashtra).",
            fundingSponsor = "Central + State Crop Risk Pool",
            eligibleStates = listOf("ALL"),
            maxLandAcres = 999.0,
            maxAnnualIncomeInr = 99999999L,
            targetGender = TargetGender.ALL.name,
            requiredDocuments = listOf(
                "7/12 Land Extract or Registered Tenant/Sharecropper Agreement",
                "Crop Sowing Declaration (Pik Pahani)",
                "Passport Photo (Carry to CSC / Bank branch)"
            ),
            officialSourceUrl = "https://pmfby.gov.in/",
            lastVerifiedDate = "19 Nov 2026",
            verifiedByOrg = "District Agri Insurance Office",
            active = true
        ),
        "pm_kusum_solar" to FarmerScheme(
            userId = userId,
            name = "PM-KUSUM Solar Agricultural Pump Subsidy",
            shortName = "PM-KUSUM Solar",
            department = "Ministry of New & Renewable Energy (MNRE)",
            category = SchemeCategory.SOLAR_IRRIGATION.name,
            benefitSummary = "Up to 60%–90% capital subsidy on standalone 3HP–7.5HP solar irrigation pumps, eliminating daytime diesel & grid costs.",
            fundingSponsor = "MNRE + State DISCOM Green Agri Fund",
            eligibleStates = listOf("ALL"),
            maxLandAcres = 10.0,
            maxAnnualIncomeInr = 400000L,
            targetGender = TargetGender.ALL.name,
            requiredDocuments = listOf(
                "7/12 Land Extract showing water source (well/borewell/pond)",
                "No-Pending-Grid-Connection Declaration",
                "Income Certificate for small/marginal subsidy tier"
            ),
            officialSourceUrl = "https://pmkusum.mnre.gov.in/",
            lastVerifiedDate = "18 Nov 2026",
            verifiedByOrg = "KVK Pune Energy Desk",
            active = true
        ),
        "kcc_credit_card" to FarmerScheme(
            userId = userId,
            name = "Kisan Credit Card (KCC) — 4% Effective Interest",
            shortName = "KCC Credit",
            department = "NABARD & Dept. of Financial Services",
            category = SchemeCategory.CREDIT_LOAN.name,
            benefitSummary = "Short-term crop working capital up to ₹3 Lakh with interest subvention bringing effective interest down to 4% p.a. on timely repayment.",
            fundingSponsor = "NABARD & Rural Cooperative Banks",
            eligibleStates = listOf("ALL"),
            maxLandAcres = 999.0,
            maxAnnualIncomeInr = 99999999L,
            targetGender = TargetGender.ALL.name,
            requiredDocuments = listOf(
                "7/12 Land Extract & Cropping Pattern Summary",
                "Village Talathi / Patwari Land Holding Verification",
                "Application Form at Nearest Cooperative / Rural Bank"
            ),
            officialSourceUrl = "https://www.myscheme.gov.in/schemes/kcc",
            lastVerifiedDate = "20 Nov 2026",
            verifiedByOrg = "District Co-op Credit Cell",
            active = true
        )
    )

    fun initialPartners(userId: String): List<Pair<String, PartnerFunding>> = listOf(
        "partner_mh_agri" to PartnerFunding(
            userId = userId,
            organizationName = "Maharashtra State Dept. of Agriculture",
            partnerType = PartnerType.STATE_AGRI_DEPT.name,
            regionCovered = "36 Districts • Maharashtra",
            farmersSponsoredTarget = 350000L,
            fundingContributionLabel = "B2G Annual SaaS & Scheme Sync License • ₹0 Farmer Fee",
            active = true
        ),
        "partner_sahyadri_csr" to PartnerFunding(
            userId = userId,
            organizationName = "Sahyadri Rural Impact CSR Foundation",
            partnerType = PartnerType.CSR_FUND.name,
            regionCovered = "Nashik, Pune & Satara Clusters",
            farmersSponsoredTarget = 120000L,
            fundingContributionLabel = "₹18L CSR Grant • Regional Dialect Voice AI Sponsor",
            active = true
        ),
        "partner_mahafpc_coop" to PartnerFunding(
            userId = userId,
            organizationName = "MahaFPC Seed & Fertilizer Co-op Network",
            partnerType = PartnerType.SEED_FERTILIZER_COOP.name,
            regionCovered = "420+ Farmer Producer Co-ops",
            farmersSponsoredTarget = 95000L,
            fundingContributionLabel = "Co-op Member Benefit Sponsorship • Zero Middlemen",
            active = true
        ),
        "partner_kvk_baramati" to PartnerFunding(
            userId = userId,
            organizationName = "Krishi Vigyan Kendra (KVK) Advisory Desks",
            partnerType = PartnerType.KVK_CENTER.name,
            regionCovered = "24 KVK Field Centers",
            farmersSponsoredTarget = 60000L,
            fundingContributionLabel = "Monthly Scheme Verification & Document Checklist Audits",
            active = true
        )
    )
}
