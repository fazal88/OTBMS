package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationLog(
    val id: String,
    val title: String,
    val body: String,
    val topic: String,
    val status: String,
    val createdAt: Long
)
