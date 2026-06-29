package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable
import com.olivetrust.charity.util.CommonSerializable

@Serializable
data class AuditLog(
    val auditId: String = "",
    val userId: String = "",
    val role: UserRole = UserRole.EMPLOYEE,
    val actionType: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val timestamp: Long = 0,
    val deviceId: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val oldValue: String? = null,
    val newValue: String? = null
) : CommonSerializable
