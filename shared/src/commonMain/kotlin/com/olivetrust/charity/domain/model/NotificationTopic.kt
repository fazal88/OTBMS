package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationTopic(
    val topicId: String,
    val name: String,
    val displayName: String,
    val description: String,
    val enabled: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
