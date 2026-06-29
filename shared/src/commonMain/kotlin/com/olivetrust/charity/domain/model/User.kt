package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable
import com.olivetrust.charity.util.CommonSerializable

@Serializable
enum class UserRole : CommonSerializable {
    SUPER_ADMIN,
    APPROVER,
    EMPLOYEE,
    BENEFICIARY,
    COLLECTOR
}

@Serializable
enum class UserStatus : CommonSerializable {
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
) : CommonSerializable
