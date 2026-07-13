package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable
import com.olivetrust.charity.util.CommonSerializable

@Serializable
enum class CollectionStatus : CommonSerializable {
    PENDING,
    RECEIVED
}

@Serializable
data class DonationCollection(
    val collectionId: String = "",
    val donationBoxId: String = "",
    val timestamp: Long = 0,
    val amountCollected: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val collectorId: String = "", // userId
    val collectorName: String = "",
    val remarks: String? = null,
    val status: CollectionStatus = CollectionStatus.PENDING,
    val receivedBy: String? = null,
    val receivedTimestamp: Long? = null
) : CommonSerializable
