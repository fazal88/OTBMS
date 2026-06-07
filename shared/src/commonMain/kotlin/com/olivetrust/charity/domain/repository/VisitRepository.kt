package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.VerificationVisit
import kotlinx.coroutines.flow.Flow

interface VisitRepository {
    fun getVisits(): Flow<List<VerificationVisit>>
    fun getVisitsByEmployee(employeeId: String): Flow<List<VerificationVisit>>
    
    suspend fun recordVisit(visit: VerificationVisit): Result<String>
}
