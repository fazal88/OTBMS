package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.ApprovalRecord
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import kotlinx.coroutines.flow.Flow

interface BeneficiaryRepository {
    fun getBeneficiaries(): Flow<List<Beneficiary>>
    fun getBeneficiaryById(id: String): Flow<Beneficiary?>
    fun getApprovals(): Flow<List<ApprovalRecord>>
    
    suspend fun createBeneficiary(beneficiary: Beneficiary): Result<String>
    suspend fun updateBeneficiary(beneficiary: Beneficiary): Result<Unit>
    suspend fun approveBeneficiary(
        id: String, 
        approverId: String, 
        notes: String, 
        natureOfAid: String,
        monthlyRation: String?,
        packetCount: Int?,
        monetaryAidAmount: Double?,
        monitorId: String
    ): Result<Unit>
    suspend fun rejectBeneficiary(id: String, rejectedBy: String, reason: String): Result<Unit>
    suspend fun requestEdit(id: String, notes: String): Result<Unit>
    suspend fun updateStatus(id: String, status: BeneficiaryStatus): Result<Unit>
    suspend fun deleteBeneficiary(id: String): Result<Unit>
}
