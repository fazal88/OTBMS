package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.AuditLog
import com.olivetrust.charity.domain.model.VerificationVisit
import com.olivetrust.charity.domain.repository.AuditRepository
import com.olivetrust.charity.domain.repository.VisitRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreVisitRepository(
    private val auditRepository: AuditRepository
) : VisitRepository {
    private val firestore by lazy { Firebase.firestore }
    private val collection by lazy { firestore.collection("verificationVisits") }

    override fun getVisits(): Flow<List<VerificationVisit>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { 
                try {
                    it.data(VerificationVisit.serializer())
                } catch (e: Exception) {
                    println("FIRESTORE_ERROR: Failed to decode visit ${it.id}: ${e.message}")
                    null
                }
            }
        }
    }

    override fun getVisitsByEmployee(employeeId: String): Flow<List<VerificationVisit>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { 
                try {
                    it.data(VerificationVisit.serializer())
                } catch (e: Exception) {
                    println("FIRESTORE_ERROR: Failed to decode visit ${it.id}: ${e.message}")
                    null
                }
            }.filter { it.employeeId == employeeId }
        }
    }

    override suspend fun recordVisit(visit: VerificationVisit): Result<String> {
        return try {
            println("FIRESTORE: Recording visit ${visit.visitId} for ${visit.beneficiaryId}")
            collection.document(visit.visitId).set(VerificationVisit.serializer(), visit)
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = visit.employeeId,
                role = com.olivetrust.charity.domain.model.UserRole.EMPLOYEE,
                actionType = "VISIT",
                entityType = "VERIFICATION",
                entityId = visit.visitId,
                timestamp = now,
                deviceId = ""
            ))
            println("FIRESTORE: Visit recorded successfully")
            Result.success(visit.visitId)
        } catch (e: Exception) {
            println("FIRESTORE_ERROR: Failed to record visit: ${e.message}")
            Result.failure(e)
        }
    }
}
