package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    
    suspend fun login(username: String, passwordHash: String, deviceId: String, deviceModel: String): Result<User>
    suspend fun logout()
    suspend fun requestDeviceChange(userId: String, oldDeviceId: String?, newDeviceId: String, deviceModel: String): Result<Unit>
    suspend fun checkDeviceApproval(userId: String, deviceId: String): Boolean
    suspend fun updateProfile(userId: String, fullName: String, mobileNumber: String): Result<User>
    suspend fun tryAutoLogin(): Boolean
}
