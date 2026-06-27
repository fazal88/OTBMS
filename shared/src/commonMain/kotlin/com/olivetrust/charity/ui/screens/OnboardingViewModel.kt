package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.LocationService
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.time.Clock

class OnboardingViewModel(
    private val authRepository: AuthRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val locationService: LocationService
) : ScreenModel {

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val state: StateFlow<OnboardingState> = _state

    fun submit(beneficiary: Beneficiary, isEdit: Boolean = false) {
        screenModelScope.launch {
            _state.value = OnboardingState.Loading
            val user = authRepository.currentUser.first()
            if (user == null) {
                _state.value = OnboardingState.Error("User not authenticated")
                return@launch
            }

            val location = if (!isEdit) locationService.getCurrentLocation() else null

            val finalBeneficiary = if (isEdit) {
                beneficiary
            } else {
                val now = Clock.System.now().toEpochMilliseconds()
                val currentDateTime = Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.currentSystemDefault())
                
                beneficiary.copy(
                    onboardingDate = now,
                    onboardedBy = user.userId,
                    deviceUsed = user.deviceId ?: "unknown",
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    startMonth = beneficiary.startMonth ?: currentDateTime.month.number,
                    startYear = beneficiary.startYear ?: currentDateTime.year
                )
            }

            val result = if (isEdit) {
                beneficiaryRepository.updateBeneficiary(finalBeneficiary).map { finalBeneficiary.id }
            } else {
                beneficiaryRepository.createBeneficiary(finalBeneficiary)
            }

            result.fold(
                onSuccess = { _state.value = OnboardingState.Success },
                onFailure = { _state.value = OnboardingState.Error(it.message ?: "Failed to save beneficiary") }
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
