package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable
import com.olivetrust.charity.util.CommonSerializable

@Serializable
data class ApprovalRecord(
    val approvalId: String = "",
    val date: Long = 0,
    val beneficiaryId: String = "",
    val beneficiaryName: String = "",
    val approverId: String = "",
    val approverName: String = "",
    val notes: String = "",
    val natureOfAid: String = "",
    val monthlyRation: String? = null,
    val packetCount: Int? = null,
    val monetaryAidAmount: Double? = null,
    val assignedMonitorId: String = "",
    val assignedMonitorName: String = "",
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null
) : CommonSerializable
