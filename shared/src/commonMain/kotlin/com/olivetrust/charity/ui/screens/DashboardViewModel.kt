package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.model.DonationBoxStatus
import com.olivetrust.charity.domain.model.IssueStatus
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
        if (user.role == com.olivetrust.charity.domain.model.UserRole.SUPER_ADMIN) return
        
        screenModelScope.launch {
            authRepository.updateProfile(user.userId, fullName, mobileNumber)
        }
    }

    init {
        // Auto-expire beneficiaries if needed - moved out of combine to avoid side-effect loops
        screenModelScope.launch {
            beneficiaryRepository.getBeneficiaries().collect { beneficiaries ->
                val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toLocalDateTime(TimeZone.currentSystemDefault())
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
        val beneficiaries = array[0] as List<com.olivetrust.charity.domain.model.Beneficiary>
        val visits = array[1] as List<com.olivetrust.charity.domain.model.VerificationVisit>
        val distributions = array[2] as List<com.olivetrust.charity.domain.model.AidDistribution>
        val boxes = array[3] as List<com.olivetrust.charity.domain.model.DonationBox>
        val collections = array[4] as List<com.olivetrust.charity.domain.model.DonationCollection>
        val issues = array[5] as List<com.olivetrust.charity.domain.model.DonationBoxIssue>
        val employees = array[6] as List<com.olivetrust.charity.domain.model.User>

        val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMonth = now.month.number
        val currentYear = now.year

        val monthlyDistributions = distributions.count { dist ->
            val distDate = Instant.fromEpochMilliseconds(dist.date).toLocalDateTime(TimeZone.currentSystemDefault())
            distDate.month.number == currentMonth && distDate.year == currentYear
        }

        val monthlyCollections = collections.count { coll ->
            val collDate = Instant.fromEpochMilliseconds(coll.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            collDate.month.number == currentMonth && collDate.year == currentYear
        }

        val collectionsToday = collections.count { coll ->
            val collDate = Instant.fromEpochMilliseconds(coll.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            collDate.dayOfMonth == now.dayOfMonth && collDate.month.number == currentMonth && collDate.year == currentYear
        }

        DashboardStats(
            approvedBeneficiaries = beneficiaries.count { it.status == BeneficiaryStatus.APPROVED },
            monthlyAidDistributed = monthlyDistributions,
            totalVisits = visits.size,
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
            activeDonationBoxes = boxes.count { it.status == DonationBoxStatus.APPROVED_ACTIVE },
            pendingDonationBoxes = boxes.count { it.status == DonationBoxStatus.PENDING_APPROVAL },
            rejectedDonationBoxes = boxes.count { it.status == DonationBoxStatus.REJECTED },
            outOfOrderDonationBoxes = boxes.count { it.status == DonationBoxStatus.OUT_OF_ORDER },
            decommissionedDonationBoxes = boxes.count { it.status == DonationBoxStatus.DECOMMISSIONED },
            collectionsToday = collectionsToday,
            collectionsThisMonth = monthlyCollections,
            totalAmountCollected = collections.sumOf { it.amountCollected },
            averageCollectionPerBox = if (boxes.isNotEmpty()) collections.sumOf { it.amountCollected } / boxes.size.toDouble() else 0.0,
            reportedIssues = issues.count { it.status == IssueStatus.PENDING_REVIEW },

            // Employee stats
            totalEmployees = employees.size,
            activeEmployees = employees.count { it.status == com.olivetrust.charity.domain.model.UserStatus.ACTIVE },
            pendingDeviceApprovals = employees.count { !it.deviceApproved && it.deviceId != null }
        )
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
}

data class DashboardStats(
    val approvedBeneficiaries: Int = 0,
    val monthlyAidDistributed: Int = 0,
    val totalVisits: Int = 0,
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
    val averageCollectionPerBox: Double = 0.0,
    val reportedIssues: Int = 0,

    // Employee stats
    val totalEmployees: Int = 0,
    val activeEmployees: Int = 0,
    val pendingDeviceApprovals: Int = 0
)
