package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuditLog(
    val auditId: String,
    val userId: String,
    val role: UserRole,
    val actionType: String,
    val entityType: String,
    val entityId: String,
    val timestamp: Long,
    val deviceId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val oldValue: String? = null,
    val newValue: String? = null
)
