package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val beneficiaryRepository: BeneficiaryRepository
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
            // Since we use real-time snapshots, a manual "refresh" isn't strictly necessary 
            // for the data to update, but we'll simulate a delay for UX feedback 
            // and the data will be fresh anyway.
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

    val stats: StateFlow<DashboardStats> = beneficiaryRepository.getBeneficiaries()
        .map { beneficiaries ->
            DashboardStats(
                total = beneficiaries.size,
                active = beneficiaries.count { it.status == com.olivetrust.charity.domain.model.BeneficiaryStatus.APPROVED },
                pending = beneficiaries.count { it.status == com.olivetrust.charity.domain.model.BeneficiaryStatus.PENDING_APPROVAL },
                reapproval = beneficiaries.count { 
                    it.status == com.olivetrust.charity.domain.model.BeneficiaryStatus.REAPPROVAL_PENDING ||
                    it.status == com.olivetrust.charity.domain.model.BeneficiaryStatus.MISUSE_REPORTED
                },
                editRequested = beneficiaries.count { it.status == com.olivetrust.charity.domain.model.BeneficiaryStatus.EDIT_REQUESTED },
                rejected = beneficiaries.count { it.status == com.olivetrust.charity.domain.model.BeneficiaryStatus.REJECTED }
            )
        }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
}

data class DashboardStats(
    val total: Int = 0,
    val active: Int = 0,
    val pending: Int = 0,
    val reapproval: Int = 0,
    val rejected: Int = 0,
    val editRequested: Int = 0
)
