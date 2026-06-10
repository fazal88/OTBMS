package com.olivetrust.charity.ui.previews

import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.model.FamilyMember
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.UserRole
import com.olivetrust.charity.domain.model.UserStatus

object PreviewMocks {
    val mockUser = User(
        userId = "1",
        fullName = "John Doe",
        username = "johndoe",
        mobileNumber = "1234567890",
        role = UserRole.SUPER_ADMIN,
        status = UserStatus.ACTIVE
    )

    val mockBeneficiary = Beneficiary(
        id = "b1",
        headName = "Muhammad Ahmad",
        headAge = 45,
        headGender = "Male",
        headOccupation = "Laborer",
        headEducation = "Primary",
        phoneNumber = "03001234567",
        incomeSource = "Daily Wage",
        address = "House 123, Street 4, Lahore",
        areaCode = "LHR-01",
        natureOfAddress = "Rented",
        natureOfRent = "5000",
        reasonForAid = "Low income, large family",
        numberOfDependants = 5,
        status = BeneficiaryStatus.PENDING_APPROVAL,
        familyMembers = listOf(
            FamilyMember("Ayesha", "Wife", 40, "Female", "Housewife"),
            FamilyMember("Ali", "Son", 15, "Male", "Student")
        ),
        onboardedBy = "Staff 1",
        deviceUsed = "Samsung A52",
        latitude = 31.5204,
        longitude = 74.3587
    )
}
