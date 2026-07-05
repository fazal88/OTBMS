package com.olivetrust.charity.data.repository

import com.olivetrust.charity.AppConfig
import com.olivetrust.charity.Environment
import com.olivetrust.charity.domain.model.NotificationLog
import com.olivetrust.charity.domain.model.NotificationTopic
import com.olivetrust.charity.domain.repository.NotificationRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class FirestoreNotificationRepository(
    private val settings: Settings,
    private val config: AppConfig
) : NotificationRepository {
    private val firestore by lazy { Firebase.firestore }
    private val functions by lazy { Firebase.functions }
    private val messaging by lazy { Firebase.messaging }

    private val topicsCollection: CollectionReference by lazy { firestore.collection("topics") }
    private val logsCollection: CollectionReference by lazy { firestore.collection("notificationLogs") }

    override val topics: Flow<List<NotificationTopic>> = topicsCollection
        .snapshots()
        .map { snapshot ->
            snapshot.documents.map { it.data(NotificationTopic.serializer()) }
        }

    override val logs: Flow<List<NotificationLog>> = logsCollection
        .orderBy("createdAt", Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
            snapshot.documents.map { it.data(NotificationLog.serializer()) }
        }

    override suspend fun createTopic(topic: NotificationTopic): Result<Unit> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val topicWithTime = topic.copy(createdAt = now, updatedAt = now)
        topicsCollection.document(topic.topicId).set(NotificationTopic.serializer(), topicWithTime)
    }

    override suspend fun updateTopic(topic: NotificationTopic): Result<Unit> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val topicWithTime = topic.copy(updatedAt = now)
        topicsCollection.document(topic.topicId).set(NotificationTopic.serializer(), topicWithTime)
    }

    override suspend fun deleteTopic(topicId: String): Result<Unit> = runCatching {
        topicsCollection.document(topicId).delete()
    }

    override suspend fun subscribeToTopic(topicName: String): Result<Unit> = runCatching {
        if (config.environment != Environment.PRODUCTION) {
            println("NOTIFICATION_REPO: Subscription skipped in non-production environment.")
            return@runCatching
        }
        messaging.subscribeToTopic(topicName)
        settings["sub_$topicName"] = true
    }

    override suspend fun unsubscribeFromTopic(topicName: String): Result<Unit> = runCatching {
        if (config.environment != Environment.PRODUCTION) return@runCatching
        messaging.unsubscribeFromTopic(topicName)
        settings["sub_$topicName"] = false
    }

    override suspend fun isSubscribed(topicName: String): Boolean {
        return settings.getBoolean("sub_$topicName", false)
    }

    override suspend fun sendTestNotification(topicName: String, title: String, body: String): Result<Unit> {
        println("NOTIFICATION_REPO: Sending test notification for topic: $topicName")
        return try {
            val data = mapOf(
                "topic" to topicName,
                "title" to title,
                "body" to body
            )
            println("NOTIFICATION_REPO: Calling Cloud Function 'sendTestNotification' with data: $data")
            val result = functions.httpsCallable("sendTestNotification").invoke(data)
            println("NOTIFICATION_REPO: Cloud Function call successful. Result: $result")
            Result.success(Unit)
        } catch (e: Exception) {
            println("NOTIFICATION_REPO_ERROR: Cloud Function call failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getFcmToken(): String? {
        println("NOTIFICATION_REPO: Environment check - config.environment: ${config.environment}")
        if (config.environment != Environment.PRODUCTION) {
            println("NOTIFICATION_REPO: Bypassing FCM token fetch in non-production environment (${config.environment}).")
            return "Disabled in ${config.environment}"
        }
        
        println("NOTIFICATION_REPO: Attempting to get FCM token with 10s timeout...")
        return try {
            val token = withTimeoutOrNull(10.seconds) {
                messaging.getToken()
            }
            if (token != null) {
                println("NOTIFICATION_REPO: Successfully got FCM token: $token")
            } else {
                println("NOTIFICATION_REPO: Token fetch timed out after 10 seconds. Is APNs configured?")
            }
            token ?: "Timed Out"
        } catch (e: Exception) {
            println("NOTIFICATION_REPO_ERROR: Failed to get FCM token: ${e.message}")
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }
}
