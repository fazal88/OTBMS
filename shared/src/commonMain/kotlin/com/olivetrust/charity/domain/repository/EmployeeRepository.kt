package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.UserStatus
import kotlinx.coroutines.flow.Flow

interface EmployeeRepository {
    fun getEmployees(): Flow<List<User>>
    
    suspend fun createEmployee(user: User, passwordHash: String): Result<String>
    suspend fun updateEmployeeStatus(userId: String, status: UserStatus): Result<Unit>
    suspend fun approveDevice(userId: String, deviceId: String, approverId: String): Result<Unit>
}
