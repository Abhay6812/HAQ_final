package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.EligibilityCheckLog
import com.example.model.EligibilityRuleEngine
import com.example.model.FarmerScheme
import com.example.model.InputMode
import com.example.model.PartnerFunding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HaqFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    private suspend fun requireAuthenticatedUserId(expectedUserId: String): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(user.uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        // Ensure Firebase Auth ID token is materialized before Firestore opens gRPC listeners
        try {
            user.getIdToken(false).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Proceed and let Firestore SDK report real auth/network status if token refresh fails
        }
        return user.uid
    }

    fun observeSchemes(userId: String): Flow<List<FarmerScheme>> = flow {
        val uid = requireAuthenticatedUserId(userId)
        val path = "users/$uid/schemes"
        emitAll(
            db.collection("users").document(uid).collection("schemes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FarmerScheme::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.shortName.lowercase() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCheckLogs(userId: String): Flow<List<EligibilityCheckLog>> = flow {
        val uid = requireAuthenticatedUserId(userId)
        val path = "users/$uid/check_logs"
        emitAll(
            db.collection("users").document(uid).collection("check_logs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EligibilityCheckLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePartnerFunding(userId: String): Flow<List<PartnerFunding>> = flow {
        val uid = requireAuthenticatedUserId(userId)
        val path = "users/$uid/partner_funding"
        emitAll(
            db.collection("users").document(uid).collection("partner_funding")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PartnerFunding::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.farmersSponsoredTarget }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedDefaultsIfEmpty(userId: String): Result<Unit> {
        val basePath = "users/$userId"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            val schemesCol = db.collection("users").document(uid).collection("schemes")
            val existingSchemes = schemesCol.limit(1).get().await()
            if (existingSchemes.isEmpty) {
                val batch = db.batch()
                DefaultSchemeCatalog.initialSchemes(uid).forEach { (docId, scheme) ->
                    batch.set(schemesCol.document(docId), schemeCreateMap(uid, scheme))
                }
                batch.commit().await()
            }

            val partnersCol = db.collection("users").document(uid).collection("partner_funding")
            val existingPartners = partnersCol.limit(1).get().await()
            if (existingPartners.isEmpty) {
                val batch = db.batch()
                DefaultSchemeCatalog.initialPartners(uid).forEach { (docId, partner) ->
                    batch.set(partnersCol.document(docId), partnerCreateMap(uid, partner))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    suspend fun createScheme(userId: String, scheme: FarmerScheme): Result<Unit> {
        val colPath = "users/$userId/schemes"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            validateSchemeInputs(scheme)
            val docRef = db.collection("users").document(uid).collection("schemes").document()
            docRef.set(schemeCreateMap(uid, scheme)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    suspend fun updateScheme(userId: String, schemeId: String, scheme: FarmerScheme): Result<Unit> {
        val docPath = "users/$userId/schemes/$schemeId"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            require(schemeId.isNotBlank()) { "Scheme ID cannot be blank" }
            validateSchemeInputs(scheme)
            val docRef = db.collection("users").document(uid).collection("schemes").document(schemeId)
            val updateMap = mapOf(
                "name" to scheme.name.trim().take(140),
                "shortName" to scheme.shortName.trim().take(40),
                "department" to scheme.department.trim().take(140),
                "category" to scheme.category,
                "benefitSummary" to scheme.benefitSummary.trim().take(240),
                "fundingSponsor" to scheme.fundingSponsor.trim().take(120),
                "eligibleStates" to scheme.eligibleStates.map { it.trim() }.filter { it.isNotEmpty() }.take(40).ifEmpty { listOf("ALL") },
                "maxLandAcres" to scheme.maxLandAcres.coerceIn(0.0, 10000.0),
                "maxAnnualIncomeInr" to scheme.maxAnnualIncomeInr.coerceIn(0L, 100_000_000L),
                "targetGender" to scheme.targetGender,
                "requiredDocuments" to scheme.requiredDocuments.map { it.trim() }.filter { it.isNotEmpty() }.take(20).ifEmpty { listOf("7/12 Land Extract") },
                "officialSourceUrl" to normalizeHttpsUrl(scheme.officialSourceUrl),
                "lastVerifiedDate" to scheme.lastVerifiedDate.trim().take(40).ifEmpty { EligibilityRuleEngine.todayFormattedDate() },
                "verifiedByOrg" to scheme.verifiedByOrg.trim().take(100).ifEmpty { "KVK Verification Desk" },
                "active" to scheme.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markSchemeVerifiedToday(
        userId: String,
        schemeId: String,
        verifiedByOrg: String
    ): Result<Unit> {
        val docPath = "users/$userId/schemes/$schemeId"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            require(schemeId.isNotBlank()) { "Scheme ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("schemes").document(schemeId)
            docRef.update(
                mapOf(
                    "lastVerifiedDate" to EligibilityRuleEngine.todayFormattedDate(),
                    "verifiedByOrg" to verifiedByOrg.trim().take(100).ifEmpty { "KVK Official Desk" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSchemeActive(userId: String, schemeId: String, active: Boolean): Result<Unit> {
        val docPath = "users/$userId/schemes/$schemeId"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            require(schemeId.isNotBlank()) { "Scheme ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("schemes").document(schemeId)
            docRef.update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteScheme(userId: String, schemeId: String): Result<Unit> {
        val docPath = "users/$userId/schemes/$schemeId"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            require(schemeId.isNotBlank()) { "Scheme ID cannot be blank" }
            db.collection("users").document(uid).collection("schemes").document(schemeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logEligibilityCheck(
        userId: String,
        stateRegion: String,
        inputMode: InputMode,
        matchedCount: Int,
        partnerChannel: String = "KVK Field Desk"
    ): Result<String> {
        val colPath = "users/$userId/check_logs"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            val docRef = db.collection("users").document(uid).collection("check_logs").document()
            val payload = mapOf(
                "userId" to uid,
                "stateRegion" to stateRegion.trim().take(60).ifEmpty { "Maharashtra" },
                "inputMode" to inputMode.name,
                "matchedCount" to matchedCount.coerceIn(0, 100),
                "checklistDownloaded" to false,
                "partnerChannel" to partnerChannel.trim().take(80).ifEmpty { "KVK Field Desk" },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    suspend fun markChecklistDownloaded(userId: String, logId: String): Result<Unit> {
        val docPath = "users/$userId/check_logs/$logId"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            require(logId.isNotBlank()) { "Log ID cannot be blank" }
            db.collection("users").document(uid).collection("check_logs").document(logId)
                .update(
                    mapOf(
                        "checklistDownloaded" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addPartnerFunding(userId: String, partner: PartnerFunding): Result<Unit> {
        val colPath = "users/$userId/partner_funding"
        return try {
            val uid = requireAuthenticatedUserId(userId)
            require(partner.organizationName.isNotBlank()) { "Organization name is required" }
            val docRef = db.collection("users").document(uid).collection("partner_funding").document()
            docRef.set(partnerCreateMap(uid, partner)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    private fun validateSchemeInputs(scheme: FarmerScheme) {
        require(scheme.name.isNotBlank()) { "Scheme name is required" }
        require(scheme.shortName.isNotBlank()) { "Short name is required" }
        require(scheme.department.isNotBlank()) { "Department is required" }
        require(scheme.benefitSummary.isNotBlank()) { "Benefit summary is required" }
        val normalizedUrl = normalizeHttpsUrl(scheme.officialSourceUrl)
        require(normalizedUrl.startsWith("https://") && normalizedUrl.length >= 10) {
            "Official source URL must be a valid https:// portal link"
        }
    }

    private fun normalizeHttpsUrl(url: String): String {
        val trimmed = url.trim().take(300)
        return when {
            trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("http://") -> "https://" + trimmed.removePrefix("http://")
            trimmed.isNotBlank() -> "https://$trimmed"
            else -> "https://www.myscheme.gov.in/"
        }
    }

    private fun schemeCreateMap(uid: String, scheme: FarmerScheme): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to scheme.name.trim().take(140),
        "shortName" to scheme.shortName.trim().take(40),
        "department" to scheme.department.trim().take(140),
        "category" to scheme.category,
        "benefitSummary" to scheme.benefitSummary.trim().take(240),
        "fundingSponsor" to scheme.fundingSponsor.trim().take(120).ifEmpty { "State Agriculture Dept" },
        "eligibleStates" to scheme.eligibleStates.map { it.trim() }.filter { it.isNotEmpty() }.take(40).ifEmpty { listOf("ALL") },
        "maxLandAcres" to scheme.maxLandAcres.coerceIn(0.0, 10000.0),
        "maxAnnualIncomeInr" to scheme.maxAnnualIncomeInr.coerceIn(0L, 100_000_000L),
        "targetGender" to scheme.targetGender,
        "requiredDocuments" to scheme.requiredDocuments.map { it.trim() }.filter { it.isNotEmpty() }.take(20).ifEmpty { listOf("7/12 Land Extract") },
        "officialSourceUrl" to normalizeHttpsUrl(scheme.officialSourceUrl),
        "lastVerifiedDate" to scheme.lastVerifiedDate.trim().take(40).ifEmpty { EligibilityRuleEngine.todayFormattedDate() },
        "verifiedByOrg" to scheme.verifiedByOrg.trim().take(100).ifEmpty { "KVK Verification Desk" },
        "active" to scheme.active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun partnerCreateMap(uid: String, partner: PartnerFunding): Map<String, Any> = mapOf(
        "userId" to uid,
        "organizationName" to partner.organizationName.trim().take(120),
        "partnerType" to partner.partnerType,
        "regionCovered" to partner.regionCovered.trim().take(100).ifEmpty { "India" },
        "farmersSponsoredTarget" to partner.farmersSponsoredTarget.coerceIn(0L, 10_000_000L),
        "fundingContributionLabel" to partner.fundingContributionLabel.trim().take(100).ifEmpty { "Zero Farmer Fee Sponsor" },
        "active" to partner.active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
