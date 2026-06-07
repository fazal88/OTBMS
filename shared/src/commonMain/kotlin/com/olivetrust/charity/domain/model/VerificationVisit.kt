package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VerificationVisit(
    val visitId: String,
    val date: Long,
    val latitude: Double,
    val longitude: Double,
    val employeeId: String,
    val beneficiaryId: String,
    val visitStatus: VisitStatus,
    val misuseReport: MisuseReport? = null,
    val editRequest: EditRequest? = null,
    val reapprovalReason: String? = null
)

@Serializable
enum class VisitStatus {
    SUCCESSFUL,
    MISUSE_REPORTED,
    EDIT_REQUESTED,
    REAPPROVAL_REQUIRED
}

@Serializable
data class MisuseReport(
    val description: String,
    val photoEvidenceUrl: String
)

@Serializable
data class EditRequest(
    val requestedChange: String,
    val supportingNotes: String
)
