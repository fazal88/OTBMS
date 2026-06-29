package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable
import com.olivetrust.charity.util.CommonSerializable

@Serializable
enum class RequestStatus : CommonSerializable {
    PENDING,
    APPROVED,
    REJECTED
}

@Serializable
data class DeviceChangeRequest(
    val requestId: String = "",
    val userId: String = "",
    val oldDeviceId: String? = null,
    val newDeviceId: String = "",
    val deviceModel: String = "",
    val requestedAt: Long = 0,
    val status: RequestStatus = RequestStatus.PENDING,
    val approvedBy: String? = null,
    val approvedAt: Long? = null
) : CommonSerializable
