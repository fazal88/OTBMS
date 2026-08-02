package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.AuditLog
import com.olivetrust.charity.domain.model.SystemTopics
import com.olivetrust.charity.domain.model.VerificationVisit
import com.olivetrust.charity.domain.repository.AuditRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.NotificationRepository
import com.olivetrust.charity.domain.repository.VisitRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class FirestoreVisitRepository(
    private val auditRepository: AuditRepository,
    private val notificationRepository: NotificationRepository,
    private val beneficiaryRepository: BeneficiaryRepository
) : VisitRepository {
    private val firestore by lazy { Firebase.firestore }
    private val collection by lazy { firestore.collection("visits") }

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
            collection.document(visit.visitId).set(VerificationVisit.serializer(), visit)
            
            val now = Clock.System.now().toEpochMilliseconds()
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = visit.employeeId,
                role = com.olivetrust.charity.domain.model.UserRole.EMPLOYEE,
                actionType = "VISIT",
                entityType = "BENEFICIARY",
                entityId = visit.beneficiaryId,
                timestamp = now,
                deviceId = ""
            ))

            // Update last visit date on beneficiary
            beneficiaryRepository.updateLastVisitDate(visit.beneficiaryId, visit.date)

            notificationRepository.sendNotification(
                topicName = SystemTopics.VERIFY_VISIT,
                title = "Verification Visit Recorded",
                body = "Visit recorded for beneficiary ${visit.beneficiaryName} by ${visit.employeeId}."
            )

            Result.success(visit.visitId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
