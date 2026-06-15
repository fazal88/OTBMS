package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeliveryStatus {
    DELIVERED,
    PARTIALLY_DELIVERED,
    FAILED
}

@Serializable
data class AidDistribution(
    val distributionId: String = "",
    val date: Long = 0,
    val beneficiaryId: String = "",
    val beneficiaryName: String = "",
    val areaCode: String = "",
    val natureOfAid: String = "",
    val aidAmount: Double = 0.0,
    val packetCount: Int = 0,
    val reason: String = "",
    val familyCount: Int = 0,
    val receiverName: String = "",
    val distributedBy: String = "", // userId
    val distributionLocationLat: Double = 0.0,
    val distributionLocationLng: Double = 0.0,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.DELIVERED,
    val evidencePhotoUrl: String? = null,
    val signatureUrl: String? = null,
    val eventId: String? = null
)
