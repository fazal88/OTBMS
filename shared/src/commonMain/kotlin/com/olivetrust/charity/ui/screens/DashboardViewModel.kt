package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val visitRepository: VisitRepository,
    private val aidRepository: AidRepository,
    private val donationBoxRepository: DonationBoxRepository,
    private val employeeRepository: EmployeeRepository
) : ScreenModel {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val currentUser = authRepository.currentUser
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout() {
        screenModelScope.launch {
            authRepository.logout()
        }
    }

    fun refresh() {
        screenModelScope.launch {
            _isRefreshing.value = true
            delay(1000)
            _isRefreshing.value = false
        }
    }

    fun updateProfile(fullName: String, mobileNumber: String) {
        val user = currentUser.value ?: return
        if (user.role == UserRole.SUPER_ADMIN) return
        
        screenModelScope.launch {
            authRepository.updateProfile(user.userId, fullName, mobileNumber)
        }
    }

    init {
        // Auto-expire beneficiaries if needed - moved out of combine to avoid side-effect loops
        screenModelScope.launch {
            beneficiaryRepository.getBeneficiaries().collect { beneficiaries ->
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val currentMonth = now.month.number
                val currentYear = now.year

                beneficiaries.forEach { b ->
                    if (b.status == BeneficiaryStatus.APPROVED && b.expiryMonth != null && b.expiryYear != null) {
                        if (currentYear > b.expiryYear || (currentYear == b.expiryYear && currentMonth > b.expiryMonth)) {
                            beneficiaryRepository.updateStatus(b.id, BeneficiaryStatus.EXPIRED)
                        }
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val stats: StateFlow<DashboardStats> = combine(
        beneficiaryRepository.getBeneficiaries(),
        visitRepository.getVisits(),
        aidRepository.getDistributions(),
        donationBoxRepository.getDonationBoxes(),
        donationBoxRepository.getAllCollections(),
        donationBoxRepository.getAllIssues(),
        employeeRepository.getEmployees()
    ) { array ->
        val beneficiaries = array[0] as List<Beneficiary>
        val visits = array[1] as List<VerificationVisit>
        val distributions = array[2] as List<AidDistribution>
        val boxes = array[3] as List<DonationBox>
        val collections = array[4] as List<DonationCollection>
        val issues = array[5] as List<DonationBoxIssue>
        val employees = array[6] as List<User>

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMonth = now.month.number
        val currentYear = now.year

        val monthlyDists = distributions.filter { dist ->
            val distDate = Instant.fromEpochMilliseconds(dist.date).toLocalDateTime(TimeZone.currentSystemDefault())
            distDate.month.number == currentMonth && distDate.year == currentYear && dist.eventId == null
        }

        val monthlyRationCount = monthlyDists.count { it.natureOfAid.equals("Ration", ignoreCase = true) }
        val monthlyMonetaryCount = monthlyDists.count { it.natureOfAid.equals("Monetary", ignoreCase = true) }
        val monthlyMonetaryAmount = monthlyDists.filter { it.natureOfAid.equals("Monetary", ignoreCase = true) }.sumOf { it.aidAmount }
        val monthlyBothCount = monthlyDists.count { it.natureOfAid.equals("Both", ignoreCase = true) }
        val monthlyBothAmount = monthlyDists.filter { it.natureOfAid.equals("Both", ignoreCase = true) }.sumOf { it.aidAmount }
        val monthlyMedicalCount = monthlyDists.count { it.natureOfAid.equals("Medical", ignoreCase = true) }
        val monthlyMedicalAmount = monthlyDists.filter { it.natureOfAid.equals("Medical", ignoreCase = true) }.sumOf { it.aidAmount }
        val monthlyEducationCount = monthlyDists.count { it.natureOfAid.equals("Education", ignoreCase = true) }
        val monthlyEducationAmount = monthlyDists.filter { it.natureOfAid.equals("Education", ignoreCase = true) }.sumOf { it.aidAmount }

        val monthlyVisits = visits.count { visit ->
            val visitDate = Instant.fromEpochMilliseconds(visit.date).toLocalDateTime(TimeZone.currentSystemDefault())
            visitDate.month.number == currentMonth && visitDate.year == currentYear
        }

        val monthlyCollections = collections.count { coll ->
            val collDate = Instant.fromEpochMilliseconds(coll.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            collDate.month.number == currentMonth && collDate.year == currentYear
        }

        val collectionsToday = collections.count { coll ->
            val collDate = Instant.fromEpochMilliseconds(coll.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            collDate.day == now.day && collDate.month.number == currentMonth && collDate.year == currentYear
        }

        DashboardStats(
            approvedBeneficiaries = beneficiaries.count { it.status == BeneficiaryStatus.APPROVED },
            monthlyRationCount = monthlyRationCount,
            monthlyMonetaryCount = monthlyMonetaryCount + monthlyBothCount,
            monthlyMonetaryAmount = monthlyMonetaryAmount + monthlyBothAmount,
            monthlyMedicalCount = monthlyMedicalCount,
            monthlyMedicalAmount = monthlyMedicalAmount,
            monthlyEducationCount = monthlyEducationCount,
            monthlyEducationAmount = monthlyEducationAmount,
            monthlyVisits = monthlyVisits,
            pendingOnboarding = beneficiaries.count { it.status == BeneficiaryStatus.PENDING_APPROVAL },
            pendingEdits = beneficiaries.count { it.status == BeneficiaryStatus.EDIT_REQUESTED },
            pendingReapprovals = beneficiaries.count { it.status == BeneficiaryStatus.REAPPROVAL_PENDING },
            misuseReports = beneficiaries.count { it.status == BeneficiaryStatus.MISUSE_REPORTED },
            rejectedBeneficiaries = beneficiaries.count { it.status == BeneficiaryStatus.REJECTED },
            deactivatedBeneficiaries = beneficiaries.count { it.status == BeneficiaryStatus.DEACTIVATED },
            draftBeneficiaries = beneficiaries.count { it.status == BeneficiaryStatus.DRAFT },
            expiredBeneficiaries = beneficiaries.count { it.status == BeneficiaryStatus.EXPIRED },
            totalBeneficiaries = beneficiaries.size,
            
            // Donation Box stats
            totalDonationBoxes = boxes.size,
            activeDonationBoxes = boxes.count { it.status == DonationBoxStatus.ACTIVE },
            pendingDonationBoxes = boxes.count { it.status == DonationBoxStatus.PENDING_APPROVAL },
            rejectedDonationBoxes = boxes.count { it.status == DonationBoxStatus.INACTIVE },
            outOfOrderDonationBoxes = boxes.count { it.status == DonationBoxStatus.INACTIVE },
            decommissionedDonationBoxes = boxes.count { it.status == DonationBoxStatus.INACTIVE },
            collectionsToday = collectionsToday,
            collectionsThisMonth = monthlyCollections,
            totalAmountCollected = collections.sumOf { it.amountCollected },
            totalAmountReceived = collections.filter { it.status == CollectionStatus.RECEIVED }.sumOf { it.amountCollected },
            pendingCollections = collections.count { it.status == CollectionStatus.PENDING },
            averageCollectionPerBox = if (boxes.isNotEmpty()) collections.sumOf { it.amountCollected } / boxes.size.toDouble() else 0.0,
            reportedIssues = issues.count { it.status == IssueStatus.PENDING_REVIEW },

            // Employee stats
            totalEmployees = employees.size,
            activeEmployees = employees.count { it.status == UserStatus.ACTIVE },
            pendingDeviceApprovals = employees.count { !it.deviceApproved && it.deviceId != null }
        )
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
}

data class DashboardStats(
    val approvedBeneficiaries: Int = 0,
    val monthlyRationCount: Int = 0,
    val monthlyMonetaryCount: Int = 0,
    val monthlyMonetaryAmount: Double = 0.0,
    val monthlyMedicalCount: Int = 0,
    val monthlyMedicalAmount: Double = 0.0,
    val monthlyEducationCount: Int = 0,
    val monthlyEducationAmount: Double = 0.0,
    val monthlyVisits: Int = 0,
    val pendingOnboarding: Int = 0,
    val pendingEdits: Int = 0,
    val pendingReapprovals: Int = 0,
    val misuseReports: Int = 0,
    val rejectedBeneficiaries: Int = 0,
    val deactivatedBeneficiaries: Int = 0,
    val draftBeneficiaries: Int = 0,
    val expiredBeneficiaries: Int = 0,
    val totalBeneficiaries: Int = 0,
    
    // Donation Box stats
    val totalDonationBoxes: Int = 0,
    val activeDonationBoxes: Int = 0,
    val pendingDonationBoxes: Int = 0,
    val rejectedDonationBoxes: Int = 0,
    val outOfOrderDonationBoxes: Int = 0,
    val decommissionedDonationBoxes: Int = 0,
    val collectionsToday: Int = 0,
    val collectionsThisMonth: Int = 0,
    val totalAmountCollected: Double = 0.0,
    val totalAmountReceived: Double = 0.0,
    val pendingCollections: Int = 0,
    val averageCollectionPerBox: Double = 0.0,
    val reportedIssues: Int = 0,

    // Employee stats
    val totalEmployees: Int = 0,
    val activeEmployees: Int = 0,
    val pendingDeviceApprovals: Int = 0
)
