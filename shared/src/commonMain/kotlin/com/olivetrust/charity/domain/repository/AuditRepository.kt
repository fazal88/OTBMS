package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.AuditLog
import kotlinx.coroutines.flow.Flow

interface AuditRepository {
    fun getLogs(): Flow<List<AuditLog>>
    fun getLogsByEntity(entityType: String, entityId: String): Flow<List<AuditLog>>
    
    suspend fun logAction(log: AuditLog): Result<Unit>
}
