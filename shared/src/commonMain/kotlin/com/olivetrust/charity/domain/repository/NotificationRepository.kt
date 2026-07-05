package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.NotificationLog
import com.olivetrust.charity.domain.model.NotificationTopic
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val topics: Flow<List<NotificationTopic>>
    val logs: Flow<List<NotificationLog>>

    suspend fun createTopic(topic: NotificationTopic): Result<Unit>
    suspend fun updateTopic(topic: NotificationTopic): Result<Unit>
    suspend fun deleteTopic(topicId: String): Result<Unit>

    suspend fun subscribeToTopic(topicName: String): Result<Unit>
    suspend fun unsubscribeFromTopic(topicName: String): Result<Unit>
    suspend fun isSubscribed(topicName: String): Boolean

    suspend fun sendTestNotification(topicName: String, title: String, body: String): Result<Unit>
    
    suspend fun sendNotification(topicName: String, title: String, body: String): Result<Unit>

    suspend fun getFcmToken(): String?
}
