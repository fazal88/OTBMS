package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable
import com.olivetrust.charity.util.CommonSerializable

@Serializable
enum class DonationBoxStatus : CommonSerializable {
    PENDING_APPROVAL,
    APPROVED_ACTIVE,
    REJECTED,
    OUT_OF_ORDER,
    DECOMMISSIONED
}

@Serializable
data class DonationBox(
    val id: String = "",
    val address: String = "",
    val personOfContact: String = "",
    val contactNumber: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val areaCode: String = "",
    val installationDate: Long = 0,
    val installedBy: String = "", // userId of Collector
    val status: DonationBoxStatus = DonationBoxStatus.PENDING_APPROVAL,
    val lastUpdated: Long = 0,
    val lastCollectionDate: Long? = null,
    val lastCollectedAmount: Double? = null,
    val remarks: String? = null,
    val rejectionReason: String? = null
) : CommonSerializable
