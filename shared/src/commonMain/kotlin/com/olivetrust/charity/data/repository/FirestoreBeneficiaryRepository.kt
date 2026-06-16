package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.*
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
    private val approvalsCollection by lazy { firestore.collection("approvals") }
    private val usersCollection by lazy { firestore.collection("users") }

    override fun getBeneficiaries(): Flow<List<Beneficiary>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { 
                try {
                    it.data(Beneficiary.serializer())
                } catch (e: Exception) {
                    println("FIRESTORE_ERROR: Failed to decode beneficiary ${it.id}: ${e.message}")
                    null
                }
            }
        }
    }

    override fun getBeneficiaryById(id: String): Flow<Beneficiary?> {
        return collection.document(id).snapshots().map { 
            try {
                if (it.exists) it.data(Beneficiary.serializer()) else null 
            } catch (e: Exception) {
                println("FIRESTORE_ERROR: Failed to decode beneficiary $id: ${e.message}")
                null
            }
        }
    }

    override fun getApprovals(): Flow<List<ApprovalRecord>> {
        return approvalsCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull {
                try {
                    it.data(ApprovalRecord.serializer())
                } catch (e: Exception) {
                    println("FIRESTORE_ERROR: Failed to decode approval ${it.id}: ${e.message}")
                    null
                }
            }
        }
    }

    private suspend fun log(beneficiary: Beneficiary, action: String, userId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        auditRepository.logAction(AuditLog(
            auditId = "A_$now",
            userId = userId,
            role = UserRole.EMPLOYEE,
            actionType = action,
            entityType = "BENEFICIARY",
            entityId = beneficiary.id,
            timestamp = now,
            deviceId = beneficiary.deviceUsed
        ))
    }

    override suspend fun createBeneficiary(beneficiary: Beneficiary): Result<String> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val finalBeneficiary = beneficiary.copy(onboardingDate = now, lastUpdated = now)
            collection.document(finalBeneficiary.id).set(Beneficiary.serializer(), finalBeneficiary)
            log(finalBeneficiary, "CREATE", finalBeneficiary.onboardedBy)
            Result.success(finalBeneficiary.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBeneficiary(beneficiary: Beneficiary): Result<Unit> {
        return try {
            val updated = beneficiary.copy(lastUpdated = Clock.System.now().toEpochMilliseconds())
            collection.document(updated.id).set(Beneficiary.serializer(), updated)
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
        monitorId: String,
        expiryMonth: Int?,
        expiryYear: Int?
    ): Result<Unit> {
        if (id.isBlank()) return Result.failure(Exception("Beneficiary ID cannot be blank"))
        return try {
            val doc = collection.document(id).get()
            val beneficiary = doc.data(Beneficiary.serializer())
            
            // Try to get names for the record safely
            val approverName = if (approverId.isNotBlank()) {
                val approverDoc = usersCollection.document(approverId).get()
                if (approverDoc.exists) approverDoc.data(User.serializer()).fullName else "Unknown Approver"
            } else "Unknown Approver"
            
            val monitorName = if (monitorId.isNotBlank()) {
                val monitorDoc = usersCollection.document(monitorId).get()
                if (monitorDoc.exists) monitorDoc.data(User.serializer()).fullName else "Unknown Monitor"
            } else "Unknown Monitor"

            val now = Clock.System.now().toEpochMilliseconds()
            
            // 1. Update Beneficiary
            val updated = beneficiary.copy(
                status = BeneficiaryStatus.APPROVED,
                approvalNotes = notes,
                natureOfAid = natureOfAid,
                monthlyRation = monthlyRation,
                packetCount = packetCount,
                monetaryAidAmount = monetaryAidAmount,
                approvedBy = approverId,
                assignedMonitor = monitorId,
                approvalDate = now,
                lastUpdated = now,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear
            )
            collection.document(id).set(Beneficiary.serializer(), updated)
            
            // 2. Create Approval Record
            val approvalRecord = ApprovalRecord(
                approvalId = "APP_${now}_${id}",
                date = now,
                beneficiaryId = id,
                beneficiaryName = beneficiary.headName,
                approverId = approverId,
                approverName = approverName,
                notes = notes,
                natureOfAid = natureOfAid,
                monthlyRation = monthlyRation,
                packetCount = packetCount,
                monetaryAidAmount = monetaryAidAmount,
                assignedMonitorId = monitorId,
                assignedMonitorName = monitorName,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear
            )
            approvalsCollection.document(approvalRecord.approvalId).set(ApprovalRecord.serializer(), approvalRecord)

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
            val now = Clock.System.now().toEpochMilliseconds()
            val updated = beneficiary.copy(
                status = BeneficiaryStatus.REJECTED,
                rejectionReason = reason,
                rejectedBy = rejectedBy,
                rejectionDate = now,
                lastUpdated = now
            )
            collection.document(id).set(Beneficiary.serializer(), updated)
            log(updated, "REJECT", rejectedBy)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestEdit(id: String, notes: String): Result<Unit> {
        return try {
            val doc = collection.document(id).get()
            val beneficiary = doc.data(Beneficiary.serializer())
            val updated = beneficiary.copy(
                status = BeneficiaryStatus.EDIT_REQUESTED,
                editRequestNotes = notes,
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )
            collection.document(id).set(Beneficiary.serializer(), updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStatus(id: String, status: BeneficiaryStatus): Result<Unit> {
        return try {
            val doc = collection.document(id).get()
            val beneficiary = doc.data(Beneficiary.serializer())
            val updated = beneficiary.copy(
                status = status,
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )
            collection.document(id).set(Beneficiary.serializer(), updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBeneficiary(id: String): Result<Unit> {
        return try {
            collection.document(id).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
