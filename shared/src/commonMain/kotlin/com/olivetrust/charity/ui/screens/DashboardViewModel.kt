package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.repository.AidRepository
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.VisitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlinx.datetime.Clock as DateClock

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val visitRepository: VisitRepository,
    private val aidRepository: AidRepository
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

    val stats: StateFlow<DashboardStats> = combine(
        beneficiaryRepository.getBeneficiaries(),
        visitRepository.getVisits(),
        aidRepository.getDistributions()
    ) { beneficiaries, visits, distributions ->
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMonth = now.month.number
        val currentYear = now.year

        // Auto-expire beneficiaries if needed
        beneficiaries.forEach { b ->
            if (b.status == BeneficiaryStatus.APPROVED && b.expiryMonth != null && b.expiryYear != null) {
                if (currentYear > b.expiryYear || (currentYear == b.expiryYear && currentMonth > b.expiryMonth)) {
                    screenModelScope.launch {
                        beneficiaryRepository.updateStatus(b.id, BeneficiaryStatus.EXPIRED)
                    }
                }
            }
        }

        val monthlyDistributions = distributions.count { dist ->
            val distDate = kotlinx.datetime.Instant.fromEpochMilliseconds(dist.date).toLocalDateTime(TimeZone.currentSystemDefault())
            distDate.month.number == currentMonth && distDate.year == currentYear
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
            totalBeneficiaries = beneficiaries.size
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
    val totalBeneficiaries: Int = 0
)
