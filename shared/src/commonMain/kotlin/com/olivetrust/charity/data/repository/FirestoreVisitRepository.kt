package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.AuditLog
import com.olivetrust.charity.domain.model.VerificationVisit
import com.olivetrust.charity.domain.repository.AuditRepository
import com.olivetrust.charity.domain.repository.VisitRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class FirestoreVisitRepository(
    private val auditRepository: AuditRepository
) : VisitRepository {
    private val firestore by lazy { Firebase.firestore }
    private val collection by lazy { firestore.collection("verificationVisits") }

    override fun getVisits(): Flow<List<VerificationVisit>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.map { it.data(VerificationVisit.serializer()) }
        }
    }

    override fun getVisitsByEmployee(employeeId: String): Flow<List<VerificationVisit>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.map { it.data(VerificationVisit.serializer()) }
                .filter { it.employeeId == employeeId }
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
                entityType = "VERIFICATION",
                entityId = visit.visitId,
                timestamp = now,
                deviceId = ""
            ))
            Result.success(visit.visitId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
