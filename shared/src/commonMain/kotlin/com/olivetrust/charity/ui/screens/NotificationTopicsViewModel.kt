package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.AppConfig
import com.olivetrust.charity.domain.model.NotificationLog
import com.olivetrust.charity.domain.model.NotificationTopic
import com.olivetrust.charity.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotificationTopicsState(
    val topics: List<NotificationTopic> = emptyList(),
    val logs: List<NotificationLog> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val fcmToken: String? = null,
    val subscribedTopicIds: Set<String> = emptySet(),
    val environment: String = ""
)

class NotificationTopicsViewModel(
    private val repository: NotificationRepository,
    private val config: AppConfig
) : ScreenModel {

    private val _state = MutableStateFlow(NotificationTopicsState(environment = config.environment.name))
    val state: StateFlow<NotificationTopicsState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Get FCM Token
            val token = repository.getFcmToken()
            _state.update { it.copy(fcmToken = token) }

            // Observe topics
            repository.topics.onEach { topics ->
                val subscriptionMap = topics.associate { it.name to repository.isSubscribed(it.name) }
                val subscribedIds = topics.filter { subscriptionMap[it.name] == true }.map { it.topicId }.toSet()
                
                _state.update { it.copy(
                    topics = topics,
                    subscribedTopicIds = subscribedIds,
                    isLoading = false
                ) }
            }.launchIn(screenModelScope)

            // Observe logs
            repository.logs.onEach { logs ->
                _state.update { it.copy(logs = logs) }
            }.launchIn(screenModelScope)
        }
    }

    fun subscribe(topic: NotificationTopic) {
        screenModelScope.launch {
            // Optimistic update
            _state.update { it.copy(subscribedTopicIds = it.subscribedTopicIds + topic.topicId) }
            
            val result = repository.subscribeToTopic(topic.name)
            if (result.isFailure) {
                // Rollback
                _state.update { it.copy(
                    subscribedTopicIds = it.subscribedTopicIds - topic.topicId,
                    error = "Failed to subscribe: ${result.exceptionOrNull()?.message}"
                ) }
            }
        }
    }

    fun unsubscribe(topic: NotificationTopic) {
        screenModelScope.launch {
            // Optimistic update
            _state.update { it.copy(subscribedTopicIds = it.subscribedTopicIds - topic.topicId) }
            
            val result = repository.unsubscribeFromTopic(topic.name)
            if (result.isFailure) {
                // Rollback
                _state.update { it.copy(
                    subscribedTopicIds = it.subscribedTopicIds + topic.topicId,
                    error = "Failed to unsubscribe: ${result.exceptionOrNull()?.message}"
                ) }
            }
        }
    }

    fun createTopic(name: String, displayName: String, description: String) {
        screenModelScope.launch {
            val topicId = "topic_${kotlin.time.Clock.System.now().toEpochMilliseconds()}"
            val topic = NotificationTopic(
                topicId = topicId,
                name = name,
                displayName = displayName,
                description = description
            )
            repository.createTopic(topic)
        }
    }

    fun updateTopic(topic: NotificationTopic) {
        screenModelScope.launch {
            repository.updateTopic(topic)
        }
    }

    fun deleteTopic(topicId: String) {
        screenModelScope.launch {
            repository.deleteTopic(topicId)
        }
    }

    fun sendTestNotification(topic: NotificationTopic) {
        println("NOTIFICATION_VM: sendTestNotification called for topic: ${topic.name}")
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            println("NOTIFICATION_VM: Launching coroutine to call repository")
            val result = repository.sendTestNotification(
                topicName = topic.name,
                title = "Test: ${topic.displayName}",
                body = "This is a test notification from the Olive Trust app."
            )
            
            println("NOTIFICATION_VM: Repository result: $result")
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                println("NOTIFICATION_VM: Error encountered: $errorMsg")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Failed to send test: $errorMsg"
                ) }
            } else {
                println("NOTIFICATION_VM: Success!")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
