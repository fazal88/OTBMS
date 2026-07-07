package com.olivetrust.charity.data.util

import com.olivetrust.charity.domain.model.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.time.Clock

object DatabaseSeeder {
    private val firestore = Firebase.firestore

    suspend fun seed() {
        try {
            println("DATABASE_SEEDER: Starting seeding...")
            val now = Clock.System.now().toEpochMilliseconds()
            seedUsers(now)
            val beneficiaries = seedBeneficiaries(now)
            seedVisits(beneficiaries, now)
            seedAidDistributions(beneficiaries, now)
            seedApprovals(beneficiaries, now)
            seedDonationBoxes(now)
            println("DATABASE_SEEDER: Seeding completed successfully!")
        } catch (e: Exception) {
            println("DATABASE_SEEDER: Error during seeding: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun seedUsers(now: Long) {
        val usersCollection = firestore.collection("users")
        val defaultPasswordHash = HashUtil.hashPassword("Password@12")

        val users = listOf(
            User(
                userId = "admin_01",
                employeeCode = "ADM001",
                username = "admin",
                fullName = "System Administrator",
                mobileNumber = "+917303991959",
                passwordHash = defaultPasswordHash,
                role = UserRole.SUPER_ADMIN,
                status = UserStatus.ACTIVE,
                createdAt = now
            ),
            User(
                userId = "employee_01",
                employeeCode = "EMP001",
                username = "emp1",
                fullName = "Nomaan Janaab",
                mobileNumber = "+918951951595",
                passwordHash = defaultPasswordHash,
                role = UserRole.EMPLOYEE,
                status = UserStatus.ACTIVE,
                createdAt = now
            ),
            User(
                userId = "employee_02",
                employeeCode = "EMP002",
                username = "emp2",
                fullName = "Fazal Shaikh",
                mobileNumber = "7303991959",
                passwordHash = defaultPasswordHash,
                role = UserRole.EMPLOYEE,
                status = UserStatus.ACTIVE,
                createdAt = now
            ),
            User(
                userId = "approver_01",
                employeeCode = "APP001",
                username = "approver",
                fullName = "Ikram Sayyed",
                mobileNumber = "+918591791959",
                passwordHash = defaultPasswordHash,
                role = UserRole.APPROVER,
                status = UserStatus.ACTIVE,
                createdAt = now
            ),
            User(
                userId = "collector_01",
                employeeCode = "COL001",
                username = "collector1",
                fullName = "Aftab Khan",
                mobileNumber = "+9807654321",
                passwordHash = defaultPasswordHash,
                role = UserRole.COLLECTOR,
                status = UserStatus.ACTIVE,
                createdAt = now
            )
        )

        users.forEach { user ->
            println("DATABASE_SEEDER: Setting user ${user.userId}...")
            usersCollection.document(user.userId).set(User.serializer(), user)
        }
    }

    private suspend fun seedBeneficiaries(now: Long): List<Beneficiary> {
        val beneficiariesCollection = firestore.collection("beneficiaries")
        val beneficiaries = mutableListOf<Beneficiary>()

        val firstNames = listOf("Muhammad", "Ahmed", "Ali", "Ayesha", "Fatima", "Omar", "Zainab", "Hassan", "Sara", "Bilal", "Usman", "Khadija", "Hamza", "Zoya", "Ibrahim")
        val lastNames = listOf("Khan", "Sheikh", "Malik", "Ahmed", "Butt", "Iqbal", "Siddiqui", "Qureshi", "Abbasi", "Raza")
        val occupations = listOf("Laborer", "Driver", "Security Guard", "Shopkeeper", "Unemployed", "Widow", "Street Vendor", "Maid", "Carpenter")
        val areas = listOf("Gate no 5", "Azmi Nagar", "Ambojwadi", "Other")
        val reasons = listOf("Low income", "Chronic illness", "Disability", "Large family with no earner", "Recent job loss", "Orphan support")
        val natureOfAidOptions = listOf("Ration", "Monetary", "Both")
        val monthlyRations = listOf("Flour 10kg, Sugar 2kg", "Oil 5L, Pulse 2kg", "Full Package Type A", "Emergency Ration")
        val relationOptions = listOf("Wife", "Son", "Daughter", "Father", "Mother", "Brother", "Sister", "Husband", "Other")
        val educations = listOf("None", "Primary", "Metric", "Intermediate")
        val dummyMobile = listOf("7303991959", "7977436375", "8591951595", "8591351595", "8591791959")

        val random = Random(42)
        for (i in 1..150) {
            val id = "ben_${i.toString().padStart(3, '0')}"
            val headName = "${firstNames.random(random)} ${lastNames.random(random)}"
            val status = BeneficiaryStatus.entries.random(random)
            val addedDate = now - (random.nextLong(1, 180) * 24 * 60 * 60 * 1000)
            val isRented = random.nextBoolean()
            val noDependents = random.nextInt(2, 5)

            val addedDateTime = kotlinx.datetime.Instant.fromEpochMilliseconds(addedDate).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())

            val fMembers = arrayListOf<FamilyMember>()
            for(j in 1..noDependents){

                val family = FamilyMember(
                    relation = "${relationOptions.random(random)}",
                    name = "${firstNames.random(random)}",
                    age = random.nextInt(1, 75),
                    gender = if (random.nextBoolean()) "Male" else "Female",
                    occupation = if (random.nextBoolean()) occupations.random(random) else "Student",
                    education = educations.random(random),
                    diseaseInability = if (random.nextFloat() > 0.8f) "Chronic Condition" else "NA",
                )

                fMembers.add(family)
            }
            val b = Beneficiary(
                id = id,
                headName = headName,
                headAge = random.nextInt(25, 75),
                headGender = if (random.nextBoolean()) "Male" else "Female",
                headOccupation = occupations.random(random),
                headEducation = educations.random(random),
                phoneNumber = dummyMobile.random(random),
                address = "House $i, Street ${random.nextInt(1, 50)}, Block ${('A'..'E').random(random)}, ${areas.random(random)}",
                areaCode = areas.random(random),
                natureOfAddress = if (isRented) "Rented" else "Owned",
                natureOfRent = if (isRented) "${random.nextInt(3, 15)}000" else null,
                reasonForAid = reasons.random(random),
                numberOfDependants = noDependents,
                onboardingDate = addedDate,
                startMonth = addedDateTime.month.number,
                startYear = addedDateTime.year,
                lastUpdated = addedDate + random.nextLong(0, 3600000),
                onboardedBy = "employee_01",
                status = status,
                latitude = 19.1 + (random.nextDouble() * 0.1),
                longitude = 72.8 + (random.nextDouble() * 0.1),
                natureOfAid = if (status == BeneficiaryStatus.APPROVED) natureOfAidOptions.random(random) else null,
                packetCount = if (status == BeneficiaryStatus.APPROVED) random.nextInt(1, 5) else null,
                monthlyRation = if (status == BeneficiaryStatus.APPROVED) monthlyRations.random(random) else null,
                monetaryAidAmount = if (status == BeneficiaryStatus.APPROVED) random.nextInt(20, 100) * 100.0 else null,
                approvalNotes = if (status == BeneficiaryStatus.APPROVED) "Verified deserving case." else null,
                familyMembers = fMembers
            )
            println("DATABASE_SEEDER: Setting beneficiary ${b.id}...")
            beneficiariesCollection.document(b.id).set(Beneficiary.serializer(), b)
            beneficiaries.add(b)
        }
        return beneficiaries
    }

    private suspend fun seedVisits(beneficiaries: List<Beneficiary>, now: Long) {
        val visitsCollection = firestore.collection("verificationVisits")
        val random = Random(42)
        
        beneficiaries.forEach { b ->
            // Most have at least 1 visit
            val visitCount = random.nextInt(1, 4)
            for (i in 1..visitCount) {
                val visitDate = b.onboardingDate + (random.nextLong(1, 10) * 24 * 60 * 60 * 1000)
                if (visitDate > now) continue
                
                val status = if (random.nextFloat() > 0.1f) VisitStatus.SUCCESSFUL else VisitStatus.REAPPROVAL_REQUIRED
                
                val visit = VerificationVisit(
                    visitId = "vis_${b.id}_$i",
                    date = visitDate,
                    latitude = b.latitude + (random.nextDouble() - 0.5) * 0.01,
                    longitude = b.longitude + (random.nextDouble() - 0.5) * 0.01,
                    employeeId = "employee_01",
                    beneficiaryId = b.id,
                    beneficiaryName = b.headName,
                    areaCode = b.areaCode,
                    visitStatus = status,
                    reapprovalReason = if (status == VisitStatus.REAPPROVAL_REQUIRED) "Living conditions improved slightly." else null
                )
                println("DATABASE_SEEDER: Setting visit ${visit.visitId}...")
                visitsCollection.document(visit.visitId).set(VerificationVisit.serializer(), visit)
            }
        }
    }

    private suspend fun seedAidDistributions(beneficiaries: List<Beneficiary>, now: Long) {
        val aidCollection = firestore.collection("aidDistributions")
        val random = Random(42)
        
        beneficiaries.filter { it.status == BeneficiaryStatus.APPROVED }.forEach { b ->
            // Approved ones have 1-5 distributions
            val distCount = random.nextInt(1, 6)
            for (i in 1..distCount) {
                val distDate = b.onboardingDate + (random.nextLong(5, 40) * 24 * 60 * 60 * 1000)
                if (distDate > now) continue

                val nature = b.natureOfAid ?: "Ration"
                val dist = AidDistribution(
                    distributionId = "dist_${b.id}_$i",
                    date = distDate,
                    beneficiaryId = b.id,
                    beneficiaryName = b.headName,
                    areaCode = b.areaCode,
                    natureOfAid = nature,
                    aidAmount = if (nature == "Monetary" || nature == "Both") (b.monetaryAidAmount ?: 5000.0) else 0.0,
                    packetCount = if (nature == "Ration" || nature == "Both") (b.packetCount ?: 1) else 0,
                    reason = "Monthly distribution",
                    familyCount = b.numberOfDependants + 1,
                    receiverName = b.headName,
                    distributedBy = "employee_01",
                    deliveryStatus = DeliveryStatus.DELIVERED
                )
                println("DATABASE_SEEDER: Setting distribution ${dist.distributionId}...")
                aidCollection.document(dist.distributionId).set(AidDistribution.serializer(), dist)
            }
        }
    }

    private suspend fun seedApprovals(beneficiaries: List<Beneficiary>, now: Long) {
        val approvalsCollection = firestore.collection("approvals")
        val random = Random(42)

        beneficiaries.filter { it.status == BeneficiaryStatus.APPROVED }.forEach { b ->
            val approvalDate = b.onboardingDate + (random.nextLong(1, 5) * 24 * 60 * 60 * 1000)
            if (approvalDate > now) return@forEach

            val record = ApprovalRecord(
                approvalId = "APP_${b.id}",
                date = approvalDate,
                beneficiaryId = b.id,
                beneficiaryName = b.headName,
                approverId = "approver_01",
                approverName = "Ikram Sayyed",
                notes = "Deserving case verified by staff.",
                natureOfAid = b.natureOfAid ?: "Ration",
                monthlyRation = b.monthlyRation,
                packetCount = b.packetCount,
                monetaryAidAmount = b.monetaryAidAmount,
                assignedMonitorId = "employee_01",
                assignedMonitorName = "Noman Janaab"
            )
            println("DATABASE_SEEDER: Setting approval record ${record.approvalId}...")
            approvalsCollection.document(record.approvalId).set(ApprovalRecord.serializer(), record)
        }
    }

    private suspend fun seedDonationBoxes(now: Long) {
        val boxCollection = firestore.collection("donation_boxes")
        val collectionsCollection = firestore.collection("collections")
        val issuesCollection = firestore.collection("donation_box_issues")
        val random = Random(42)

        val areas = listOf("Gate no 5", "Azmi Nagar", "Ambojwadi", "Malad West", "Kandivali East")
        val statusOptions = DonationBoxStatus.entries
        val dummyMobile = listOf("7303991959", "7977436375", "8591951595", "8591351595", "8591791959")

        for (i in 1..20) {
            val boxId = "DBX_${i.toString().padStart(3, '0')}"
            val status = statusOptions.random(random)
            val installationDate = now - (random.nextLong(10, 100) * 24 * 60 * 60 * 1000)
            
            val box = DonationBox(
                id = boxId,
                address = "Shop $i, Sector ${random.nextInt(1, 10)}, ${areas.random(random)}",
                personOfContact = "POC $i",
                contactNumber = dummyMobile.random(random),
                latitude = 19.1 + (random.nextDouble() * 0.1),
                longitude = 72.8 + (random.nextDouble() * 0.1),
                areaCode = areas.random(random),
                installationDate = installationDate,
                installedBy = "collector_01",
                status = status,
                lastUpdated = installationDate + 3600000
            )

            // Seed collections for approved boxes
            if (status == DonationBoxStatus.ACTIVE) {
                val collCount = random.nextInt(1, 5)
                var lastDate = installationDate
                var lastAmount = 0.0
                for (j in 1..collCount) {
                    val collDate = lastDate + (random.nextLong(7, 30) * 24 * 60 * 60 * 1000)
                    if (collDate > now) break
                    
                    val amount = random.nextInt(50, 500) * 10.0
                    val coll = DonationCollection(
                        collectionId = "COLL_${boxId}_$j",
                        donationBoxId = boxId,
                        timestamp = collDate,
                        amountCollected = amount,
                        latitude = box.latitude,
                        longitude = box.longitude,
                        collectorId = "collector_01",
                        collectorName = "Charlie Collector"
                    )
                    collectionsCollection.document(coll.collectionId).set(DonationCollection.serializer(), coll)
                    lastDate = collDate
                    lastAmount = amount
                }
                
                val updatedBox = box.copy(
                    lastCollectionDate = if (lastDate > installationDate) lastDate else null,
                    lastCollectedAmount = if (lastAmount > 0) lastAmount else null
                )
                boxCollection.document(boxId).set(DonationBox.serializer(), updatedBox)
            } else {
                boxCollection.document(boxId).set(DonationBox.serializer(), box)
            }

            // Seed some issues
            if (random.nextFloat() > 0.8f) {
                val issue = DonationBoxIssue(
                    issueId = "ISSUE_${boxId}_1",
                    donationBoxId = boxId,
                    reportType = IssueType.entries.random(random),
                    description = "Issue reported for box $boxId",
                    latitude = box.latitude,
                    longitude = box.longitude,
                    timestamp = now - 86400000,
                    collectorId = "collector_01",
                    status = IssueStatus.PENDING_REVIEW
                )
                issuesCollection.document(issue.issueId).set(DonationBoxIssue.serializer(), issue)
            }
        }
    }
}
