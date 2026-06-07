package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.model.AuditLog
import com.olivetrust.charity.domain.repository.AidRepository
import com.olivetrust.charity.domain.repository.AuditRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class FirestoreAidRepository(
    private val auditRepository: AuditRepository
) : AidRepository {
    private val firestore by lazy { Firebase.firestore }
    private val collection by lazy { firestore.collection("aidDistributions") }

    override fun getDistributions(): Flow<List<AidDistribution>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { 
                try {
                    it.data(AidDistribution.serializer())
                } catch (e: Exception) {
                    println("FIRESTORE_ERROR: Failed to decode distribution ${it.id}: ${e.message}")
                    null
                }
            }
        }
    }

    override fun getDistributionsByBeneficiary(beneficiaryId: String): Flow<List<AidDistribution>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { 
                try {
                    it.data(AidDistribution.serializer())
                } catch (e: Exception) {
                    println("FIRESTORE_ERROR: Failed to decode distribution ${it.id}: ${e.message}")
                    null
                }
            }.filter { it.beneficiaryId == beneficiaryId }
        }
    }

    override suspend fun recordDistribution(distribution: AidDistribution): Result<String> {
        return try {
            collection.document(distribution.distributionId).set(AidDistribution.serializer(), distribution)
            val now = Clock.System.now().toEpochMilliseconds()
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = distribution.distributedBy,
                role = com.olivetrust.charity.domain.model.UserRole.EMPLOYEE,
                actionType = "DISTRIBUTION",
                entityType = "AID",
                entityId = distribution.distributionId,
                timestamp = now,
                deviceId = ""
            ))
            Result.success(distribution.distributionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
