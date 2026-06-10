package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.LocationService
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.model.VerificationVisit
import com.olivetrust.charity.domain.model.VisitStatus
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.VisitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun recordVisit(visit: VerificationVisit) {
        screenModelScope.launch {
            _state.value = VisitState.Loading
            val user = authRepository.currentUser.first()
            if (user == null) {
                _state.value = VisitState.Error("User not authenticated")
                return@launch
            }

            val location = locationService.getCurrentLocation()

            val finalVisit = visit.copy(
                employeeId = user.fullName, // Consistently using name as requested for distributedBy
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0
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
