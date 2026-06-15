package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EventStatus {
    PLANNED,
    ONGOING,
    COMPLETED,
    CANCELLED
}

@Serializable
data class DistributionEvent(
    val id: String = "",
    val name: String = "",
    val date: Long = 0,
    val reason: String = "",
    val natureOfAid: String = "",
    val packetCount: Int? = null,
    val monetaryAidAmount: Double? = null,
    val inviteeIds: List<String> = emptyList(),
    val status: EventStatus = EventStatus.PLANNED,
    val createdBy: String = "",
    val createdAt: Long = 0
)
