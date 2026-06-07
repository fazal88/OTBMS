package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.VerificationVisit
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.VisitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VerificationVisitViewModel(
    private val authRepository: AuthRepository,
    private val visitRepository: VisitRepository
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

            val finalVisit = visit.copy(employeeId = user.userId)
            val result = visitRepository.recordVisit(finalVisit)
            
            result.fold(
                onSuccess = { _state.value = VisitState.Success },
                onFailure = { _state.value = VisitState.Error(it.message ?: "Failed to record visit") }
            )
        }
    }
}

sealed class VisitState {
    object Idle : VisitState()
    object Loading : VisitState()
    object Success : VisitState()
    data class Error(val message: String) : VisitState()
}
