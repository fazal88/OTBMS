package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    SUPER_ADMIN,
    APPROVER,
    EMPLOYEE,
    BENEFICIARY
}

@Serializable
enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    DISABLED
}

@Serializable
data class User(
    val userId: String = "",
    val employeeCode: String? = null,
    val username: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val passwordHash: String = "",
    val role: UserRole = UserRole.EMPLOYEE,
    val status: UserStatus = UserStatus.ACTIVE,
    val deviceId: String? = null,
    val deviceModel: String? = null,
    val deviceApproved: Boolean = false,
    val createdAt: Long = 0,
    val lastLoginAt: Long = 0
)
