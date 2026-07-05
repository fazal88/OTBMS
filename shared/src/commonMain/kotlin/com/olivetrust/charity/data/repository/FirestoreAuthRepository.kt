package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.DeviceChangeRequest
import com.olivetrust.charity.domain.model.RequestStatus
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.repository.AuthRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

class FirestoreAuthRepository(
    private val settings: Settings = Settings()
) : AuthRepository {
    private val firestore by lazy { Firebase.firestore }
    private val usersCollection by lazy { firestore.collection("users") }
    private val deviceRequestsCollection by lazy { firestore.collection("deviceChangeRequests") }
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_USER_DATA = "user_data"
    }

    override suspend fun tryAutoLogin(): Boolean {
        val savedUserJson = settings.getStringOrNull(KEY_USER_DATA)
        return if (savedUserJson != null) {
            try {
                val user = json.decodeFromString(User.serializer(), savedUserJson)
                _currentUser.value = user
                true
            } catch (e: Exception) {
                settings.remove(KEY_USER_DATA)
                false
            }
        } else {
            false
        }
    }

    override suspend fun login(username: String, passwordHash: String, deviceId: String, deviceModel: String): Result<User> {
        return try {
            val snapshot = usersCollection.get()
            val userDoc = snapshot.documents.find { 
                try {
                    val u = it.data(User.serializer())
                    u.username == username
                } catch (e: Exception) {
                    println("FIRESTORE_ERROR: Failed to decode user in list ${it.id}: ${e.message}")
                    false
                }
            } ?: return Result.failure(Exception("User not found"))
            
            val user = try {
                userDoc.data(User.serializer())
            } catch (e: Exception) {
                return Result.failure(Exception("Failed to decode user data: ${e.message}"))
            }
            
            if (user.passwordHash != passwordHash) {
                return Result.failure(Exception("Invalid password"))
            }

            if (user.deviceId != null && user.deviceId != deviceId) {
                return Result.failure(Exception("Device not approved. This account is registered on another device."))
            }
            
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            
            val updatedUser = if (user.deviceId == null) {
                user.copy(
                    deviceId = deviceId, 
                    deviceModel = deviceModel, 
                    deviceApproved = true,
                    lastLoginAt = now
                )
            } else {
                user.copy(lastLoginAt = now)
            }
            
            usersCollection.document(user.userId).set(User.serializer(), updatedUser)
            
            // Save to settings for persistence
            settings[KEY_USER_DATA] = json.encodeToString(User.serializer(), updatedUser)
            
            _currentUser.value = updatedUser
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        settings.remove(KEY_USER_DATA)
        _currentUser.value = null
    }

    override suspend fun updateProfile(userId: String, fullName: String, mobileNumber: String): Result<User> {
        return try {
            val userDoc = usersCollection.document(userId).get()
            val user = userDoc.data(User.serializer())
            
            val updatedUser = user.copy(
                fullName = fullName,
                mobileNumber = mobileNumber
            )
            
            usersCollection.document(userId).set(User.serializer(), updatedUser)
            
            // Update saved session if it's the current user
            if (_currentUser.value?.userId == userId) {
                settings[KEY_USER_DATA] = json.encodeToString(User.serializer(), updatedUser)
                _currentUser.value = updatedUser
            }
            
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestDeviceChange(userId: String, oldDeviceId: String?, newDeviceId: String, deviceModel: String): Result<Unit> {
        return try {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val requestId = "REQ_${userId}_${now}"
            val request = DeviceChangeRequest(
                requestId = requestId,
                userId = userId,
                oldDeviceId = oldDeviceId,
                newDeviceId = newDeviceId,
                deviceModel = deviceModel,
                requestedAt = now,
                status = RequestStatus.PENDING
            )
            deviceRequestsCollection.document(requestId).set(DeviceChangeRequest.serializer(), request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkDeviceApproval(userId: String, deviceId: String): Boolean {
        return try {
            val userDoc = usersCollection.document(userId).get()
            val user = userDoc.data(User.serializer())
            user.deviceId == deviceId && user.deviceApproved
        } catch (e: Exception) {
            false
        }
    }
}
