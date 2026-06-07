package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.AuditLog
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.repository.AuditRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class FirestoreBeneficiaryRepository(
    private val auditRepository: AuditRepository
) : BeneficiaryRepository {
    private val firestore by lazy { Firebase.firestore }
    private val collection by lazy { firestore.collection("beneficiaries") }

    override fun getBeneficiaries(): Flow<List<Beneficiary>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.map { it.data(Beneficiary.serializer()) }
        }
    }

    override fun getBeneficiaryById(id: String): Flow<Beneficiary?> {
        return collection.document(id).snapshots().map { 
            if (it.exists) it.data(Beneficiary.serializer()) else null 
        }
    }

    private suspend fun log(beneficiary: Beneficiary, action: String, userId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        auditRepository.logAction(AuditLog(
            auditId = "A_$now",
            userId = userId,
            role = com.olivetrust.charity.domain.model.UserRole.EMPLOYEE,
            actionType = action,
            entityType = "BENEFICIARY",
            entityId = beneficiary.id,
            timestamp = now,
            deviceId = beneficiary.deviceUsed
        ))
    }

    override suspend fun createBeneficiary(beneficiary: Beneficiary): Result<String> {
        return try {
            collection.document(beneficiary.id).set(Beneficiary.serializer(), beneficiary)
            log(beneficiary, "CREATE", beneficiary.onboardedBy)
            Result.success(beneficiary.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBeneficiary(beneficiary: Beneficiary): Result<Unit> {
        return try {
            collection.document(beneficiary.id).set(Beneficiary.serializer(), beneficiary)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveBeneficiary(
        id: String,
        approverId: String,
        notes: String,
        natureOfAid: String,
        monthlyRation: String?,
        packetCount: Int?,
        monetaryAidAmount: Double?,
        monitorId: String
    ): Result<Unit> {
        return try {
            val doc = collection.document(id).get()
            val beneficiary = doc.data(Beneficiary.serializer())
            val updated = beneficiary.copy(
                status = BeneficiaryStatus.APPROVED,
                approvalNotes = notes,
                natureOfAid = natureOfAid,
                monthlyRation = monthlyRation,
                packetCount = packetCount,
                monetaryAidAmount = monetaryAidAmount,
                approvedBy = approverId,
                assignedMonitor = monitorId,
                approvalDate = Clock.System.now().toEpochMilliseconds()
            )
            collection.document(id).set(Beneficiary.serializer(), updated)
            log(updated, "APPROVE", approverId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectBeneficiary(id: String, rejectedBy: String, reason: String): Result<Unit> {
        return try {
            val doc = collection.document(id).get()
            val beneficiary = doc.data(Beneficiary.serializer())
            val updated = beneficiary.copy(
                status = BeneficiaryStatus.REJECTED,
                rejectionReason = reason,
                rejectedBy = rejectedBy,
                rejectionDate = Clock.System.now().toEpochMilliseconds()
            )
            collection.document(id).set(Beneficiary.serializer(), updated)
            log(updated, "REJECT", rejectedBy)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStatus(id: String, status: BeneficiaryStatus): Result<Unit> {
        return try {
            val doc = collection.document(id).get()
            val beneficiary = doc.data(Beneficiary.serializer())
            val updated = beneficiary.copy(status = status)
            collection.document(id).set(Beneficiary.serializer(), updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
