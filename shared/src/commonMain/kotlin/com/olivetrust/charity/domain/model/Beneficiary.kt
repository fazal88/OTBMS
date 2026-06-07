package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BeneficiaryStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    DEACTIVATED,
    REAPPROVAL_PENDING
}

@Serializable
data class Beneficiary(
    val id: String,
    val headName: String,
    val photoUrl: String,
    val phoneNumber: String,
    val incomeSource: String,
    val address: String,
    val areaCode: String,
    val natureOfAddress: String,
    val natureOfRent: String? = null,
    val diseaseInability: String? = null,
    val reasonForAid: String,
    val numberOfDependants: Int,
    val familyMembers: List<FamilyMember> = emptyList(),
    
    // Metadata
    val onboardingDate: Long,
    val onboardedBy: String, // userId
    val latitude: Double,
    val longitude: Double,
    val deviceUsed: String,
    val status: BeneficiaryStatus = BeneficiaryStatus.PENDING_APPROVAL,
    
    // Approval Details
    val approvalNotes: String? = null,
    val natureOfAid: String? = null,
    val monthlyRation: String? = null,
    val packetCount: Int? = null,
    val monetaryAidAmount: Double? = null,
    val approvedBy: String? = null,
    val assignedMonitor: String? = null,
    val approvalDate: Long? = null,
    
    // Rejection Details
    val rejectionReason: String? = null,
    val rejectedBy: String? = null,
    val rejectionDate: Long? = null
)

@Serializable
data class FamilyMember(
    val name: String,
    val age: Int,
    val gender: String,
    val occupation: String,
    val education: String,
    val diseaseInability: String? = null
)
