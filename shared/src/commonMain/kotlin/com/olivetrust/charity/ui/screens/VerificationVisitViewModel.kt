package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.LocationService
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.model.VerificationVisit
import com.olivetrust.charity.domain.model.VisitStatus
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.VisitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VerificationVisitViewModel(
    private val authRepository: AuthRepository,
    private val visitRepository: VisitRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val locationService: LocationService
) : ScreenModel {

    private val _state = MutableStateFlow<VisitState>(VisitState.Idle)
    val state: StateFlow<VisitState> = _state

    private val _beneficiary = MutableStateFlow<Beneficiary?>(null)
    val beneficiary: StateFlow<Beneficiary?> = _beneficiary.asStateFlow()

    private val _currentLocation = MutableStateFlow<com.olivetrust.charity.Location?>(null)
    val currentLocation: StateFlow<com.olivetrust.charity.Location?> = _currentLocation.asStateFlow()

    fun loadData(beneficiaryId: String) {
        screenModelScope.launch {
            beneficiaryRepository.getBeneficiaryById(beneficiaryId).collect {
                _beneficiary.value = it
            }
        }
        screenModelScope.launch {
            _currentLocation.value = locationService.getCurrentLocation()
        }
    }

    fun recordVisit(visit: VerificationVisit) {
        screenModelScope.launch {
            _state.value = VisitState.Loading
            val user = authRepository.currentUser.first()
            val ben = _beneficiary.value
            if (user == null || ben == null) {
                _state.value = VisitState.Error("Data not fully loaded")
                return@launch
            }

            val location = _currentLocation.value ?: locationService.getCurrentLocation()
            val currentLat = location?.latitude ?: 0.0
            val currentLng = location?.longitude ?: 0.0

            val distance = com.olivetrust.charity.domain.util.LocationUtil.calculateDistance(
                ben.latitude, ben.longitude,
                currentLat, currentLng
            )

            val finalVisit = visit.copy(
                employeeId = user.fullName,
                latitude = currentLat,
                longitude = currentLng,
                beneficiaryLatitude = ben.latitude,
                beneficiaryLongitude = ben.longitude,
                distanceInMeters = distance
            )
            val result = visitRepository.recordVisit(finalVisit)
            
            if (result.isSuccess) {
                // Update beneficiary status based on visit result
                when (visit.visitStatus) {
                    VisitStatus.REAPPROVAL_REQUIRED -> {
                        beneficiaryRepository.updateStatus(visit.beneficiaryId, BeneficiaryStatus.REAPPROVAL_PENDING)
                    }
                    VisitStatus.MISUSE_REPORTED -> {
                        beneficiaryRepository.updateStatus(visit.beneficiaryId, BeneficiaryStatus.MISUSE_REPORTED)
                    }
                    VisitStatus.EDIT_REQUESTED -> {
                        beneficiaryRepository.requestEdit(visit.beneficiaryId, visit.editRequest?.supportingNotes ?: "")
                    }
                    else -> {}
                }
                _state.value = VisitState.Success
            } else {
                _state.value = VisitState.Error(result.exceptionOrNull()?.message ?: "Failed to record visit")
            }
        }
    }
}

sealed class VisitState {
    object Idle : VisitState()
    object Loading : VisitState()
    object Success : VisitState()
    data class Error(val message: String) : VisitState()
}
