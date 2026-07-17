package com.olivetrust.charity.domain.model

import kotlinx.serialization.Serializable
import com.olivetrust.charity.util.CommonSerializable

@Serializable
enum class BeneficiaryStatus : CommonSerializable {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    DEACTIVATED,
    REAPPROVAL_PENDING,
    MISUSE_REPORTED,
    EDIT_REQUESTED,
    EXPIRED
}

@Serializable
data class Beneficiary(
    val id: String = "",
    val headName: String = "",
    val headAge: Int = 0,
    val headGender: String = "",
    val headOccupation: String = "",
    val headEducation: String = "",
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val incomeSource: String = "",
    val address: String = "",
    val areaCode: String = "",
    val natureOfAddress: String = "",
    val natureOfRent: String? = null,
    val diseaseInability: String? = null,
    val reasonForAid: String = "",
    val numberOfDependants: Int = 0,
    val familyMembers: List<FamilyMember> = emptyList(),
    
    // Metadata
    val onboardingDate: Long = 0,
    val onboardedBy: String = "", // userId
    val startMonth: Int? = null,
    val startYear: Int? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val deviceUsed: String = "",
    val status: BeneficiaryStatus = BeneficiaryStatus.PENDING_APPROVAL,
    val lastUpdated: Long = 0,
    val lastVisitDate: Long? = null,
    
    // Expiry info
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null,

    // Approval Details
    val approvalNotes: String? = null,
    val natureOfAid: String? = null,
    val monthlyRation: String? = null,
    val packetCount: Int? = null,
    val monetaryAidAmount: Double? = null,
    val medicalAidAmount: Double? = null,
    val educationAidAmount: Double? = null,
    val approvedBy: String? = null,
    val assignedMonitor: String? = null,
    val approvalDate: Long? = null,
    
    // Rejection Details
    val rejectionReason: String? = null,
    val rejectedBy: String? = null,
    val rejectionDate: Long? = null,

    // Edit Request Details
    val editRequestNotes: String? = null
) : CommonSerializable

@Serializable
data class FamilyMember(
    val relation: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val occupation: String = "",
    val education: String = "",
    val diseaseInability: String? = null
) : CommonSerializable
