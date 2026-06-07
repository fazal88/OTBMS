package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock

class OnboardingViewModel(
    private val authRepository: AuthRepository,
    private val beneficiaryRepository: BeneficiaryRepository
) : ScreenModel {

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val state: StateFlow<OnboardingState> = _state

    fun submit(beneficiary: Beneficiary) {
        screenModelScope.launch {
            _state.value = OnboardingState.Loading
            val user = authRepository.currentUser.first()
            if (user == null) {
                _state.value = OnboardingState.Error("User not authenticated")
                return@launch
            }

            val finalBeneficiary = beneficiary.copy(
                onboardingDate = Clock.System.now().toEpochMilliseconds(),
                onboardedBy = user.userId,
                deviceUsed = user.deviceId ?: "unknown"
            )

            val result = beneficiaryRepository.createBeneficiary(finalBeneficiary)
            result.fold(
                onSuccess = { _state.value = OnboardingState.Success },
                onFailure = { _state.value = OnboardingState.Error(it.message ?: "Failed to create beneficiary") }
            )
        }
    }
}

sealed class OnboardingState {
    object Idle : OnboardingState()
    object Loading : OnboardingState()
    object Success : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}
