package com.olivetrust.charity.data.util

import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.model.FamilyMember
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.UserRole
import com.olivetrust.charity.domain.model.UserStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlin.time.Clock

object DatabaseSeeder {
    private val firestore = Firebase.firestore
    
    suspend fun seed() {
        try {
            println("DATABASE_SEEDER: Starting seeding...")
            val now = Clock.System.now().toEpochMilliseconds()
            seedUsers(now)
            seedBeneficiaries(now)
            println("DATABASE_SEEDER: Seeding completed successfully!")
        } catch (e: Exception) {
            println("DATABASE_SEEDER: Error during seeding: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun seedUsers(now: Long) {
        val usersCollection = firestore.collection("users")

        val users = listOf(
            User(
                userId = "admin_01",
                employeeCode = "ADM001",
                username = "admin",
                fullName = "System Administrator",
                mobileNumber = "+1234567890",
                passwordHash = "admin123",
                role = UserRole.SUPER_ADMIN,
                status = UserStatus.ACTIVE,
                createdAt = now
            ),
            User(
                userId = "approver_01",
                employeeCode = "APP001",
                username = "approver",
                fullName = "Main Approver",
                mobileNumber = "+1234567891",
                passwordHash = "approver123",
                role = UserRole.APPROVER,
                status = UserStatus.ACTIVE,
                createdAt = now
            ),
            User(
                userId = "employee_01",
                employeeCode = "EMP001",
                username = "employee",
                fullName = "Field Worker",
                mobileNumber = "+1234567892",
                passwordHash = "employee123",
                role = UserRole.EMPLOYEE,
                status = UserStatus.ACTIVE,
                createdAt = now
            )
        )

        users.forEach { user ->
            println("DATABASE_SEEDER: Setting user ${user.userId}...")
            usersCollection.document(user.userId).set(User.serializer(), user)
            println("DATABASE_SEEDER: User ${user.userId} set.")
        }
    }

    private suspend fun seedBeneficiaries(now: Long) {
        val beneficiariesCollection = firestore.collection("beneficiaries")

        val beneficiary = Beneficiary(
            id = "ben_01",
            headName = "John Doe",
            photoUrl = "https://example.com/photo.jpg",
            phoneNumber = "+1234567899",
            incomeSource = "Daily Wage Labor",
            address = "123 Charity Lane, City",
            areaCode = "AREA51",
            natureOfAddress = "Rented",
            natureOfRent = "Monthly",
            diseaseInability = "None",
            reasonForAid = "Loss of job due to health issues",
            numberOfDependants = 4,
            familyMembers = listOf(
                FamilyMember(
                    name = "Jane Doe",
                    age = 35,
                    gender = "Female",
                    occupation = "Homemaker",
                    education = "High School"
                )
            ),
            onboardingDate = now,
            onboardedBy = "employee_01",
            latitude = 34.0522,
            longitude = -118.2437,
            deviceUsed = "Simulator",
            status = BeneficiaryStatus.PENDING_APPROVAL
        )

        println("DATABASE_SEEDER: Setting beneficiary ${beneficiary.id}...")
        beneficiariesCollection.document(beneficiary.id).set(Beneficiary.serializer(), beneficiary)
        println("DATABASE_SEEDER: Beneficiary ${beneficiary.id} set.")
    }
}
