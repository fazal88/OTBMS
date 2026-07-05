package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationTopic(
    val topicId: String,
    val name: String,
    val displayName: String,
    val description: String,
    val enabled: Boolean = true,
    val isSystemTopic: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

object SystemTopics {
    const val ONBOARD = "Onboard"
    const val VERIFY_VISIT = "Verify Visit"
    const val AID_DISTRIBUTION = "aid distribution"
    const val COLLECTION = "Collection"

    val all = listOf(
        NotificationTopic(
            topicId = "sys_onboard",
            name = ONBOARD,
            displayName = "New Onboarding",
            description = "Notifications when a new beneficiary is onboarded.",
            isSystemTopic = true
        ),
        NotificationTopic(
            topicId = "sys_verify",
            name = VERIFY_VISIT,
            displayName = "Verification Visits",
            description = "Notifications when an employee records a verification visit.",
            isSystemTopic = true
        ),
        NotificationTopic(
            topicId = "sys_aid",
            name = AID_DISTRIBUTION,
            displayName = "Aid Distribution",
            description = "Notifications when aid is delivered to a beneficiary.",
            isSystemTopic = true
        ),
        NotificationTopic(
            topicId = "sys_collection",
            name = COLLECTION,
            displayName = "Donation Collections",
            description = "Notifications when funds are collected from donation boxes.",
            isSystemTopic = true
        )
    )
}
