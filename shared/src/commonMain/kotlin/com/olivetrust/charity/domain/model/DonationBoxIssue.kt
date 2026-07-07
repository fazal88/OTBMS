package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable
import com.olivetrust.charity.util.CommonSerializable

@Serializable
enum class IssueType : CommonSerializable {
    INACTIVE,
    EDIT_REQUIRED
}

@Serializable
enum class IssueStatus : CommonSerializable {
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}

@Serializable
data class DonationBoxIssue(
    val issueId: String = "",
    val donationBoxId: String = "",
    val reportType: IssueType = IssueType.INACTIVE,
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0,
    val collectorId: String = "", // userId
    val status: IssueStatus = IssueStatus.PENDING_REVIEW,
    val reviewNotes: String? = null,
    val reviewedBy: String? = null,
    val reviewTimestamp: Long? = null
) : CommonSerializable
