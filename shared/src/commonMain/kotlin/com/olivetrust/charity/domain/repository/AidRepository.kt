package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.AidDistribution
import kotlinx.coroutines.flow.Flow

interface AidRepository {
    fun getDistributions(): Flow<List<AidDistribution>>
    fun getDistributionsByBeneficiary(beneficiaryId: String): Flow<List<AidDistribution>>
    
    suspend fun recordDistribution(distribution: AidDistribution): Result<String>
}
