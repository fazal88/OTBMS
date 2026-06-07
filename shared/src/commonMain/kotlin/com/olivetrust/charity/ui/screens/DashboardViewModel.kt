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

class DashboardViewModel(
    private val authRepository: AuthRepository,
    beneficiaryRepository: BeneficiaryRepository
) : ScreenModel {

    val currentUser = authRepository.currentUser
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout() {
        screenModelScope.launch {
            authRepository.logout()
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
                pending = beneficiaries.count { it.status == com.olivetrust.charity.domain.model.BeneficiaryStatus.PENDING_APPROVAL }
            )
        }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
}

data class DashboardStats(
    val total: Int = 0,
    val active: Int = 0,
    val pending: Int = 0
)
