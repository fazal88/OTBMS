package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VerificationVisit(
    val visitId: String = "",
    val date: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val employeeId: String = "",
    val beneficiaryId: String = "",
    val beneficiaryName: String = "",
    val beneficiaryLatitude: Double = 0.0,
    val beneficiaryLongitude: Double = 0.0,
    val distanceInMeters: Double = 0.0,
    val areaCode: String = "",
    val visitStatus: VisitStatus = VisitStatus.SUCCESSFUL,
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
    val description: String = "",
    val photoEvidenceUrl: String = ""
)

@Serializable
data class EditRequest(
    val requestedChange: String = "",
    val supportingNotes: String = ""
)
