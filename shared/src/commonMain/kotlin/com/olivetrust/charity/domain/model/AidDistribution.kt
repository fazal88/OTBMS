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
    val distributionId: String,
    val date: Long,
    val beneficiaryId: String,
    val beneficiaryName: String,
    val areaCode: String,
    val natureOfAid: String,
    val aidAmount: Double,
    val packetCount: Int,
    val reason: String,
    val familyCount: Int,
    val receiverName: String,
    val distributedBy: String, // userId
    val distributionLocationLat: Double,
    val distributionLocationLng: Double,
    val deliveryStatus: DeliveryStatus,
    val evidencePhotoUrl: String? = null,
    val signatureUrl: String? = null
)
