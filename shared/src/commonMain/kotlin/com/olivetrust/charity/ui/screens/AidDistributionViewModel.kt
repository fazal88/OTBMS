package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.repository.AidRepository
import com.olivetrust.charity.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AidDistributionViewModel(
    private val authRepository: AuthRepository,
    private val aidRepository: AidRepository
) : ScreenModel {

    private val _state = MutableStateFlow<AidState>(AidState.Idle)
    val state: StateFlow<AidState> = _state

    fun recordDistribution(distribution: AidDistribution) {
        screenModelScope.launch {
            _state.value = AidState.Loading
            val user = authRepository.currentUser.first()
            if (user == null) {
                _state.value = AidState.Error("User not authenticated")
                return@launch
            }

            val finalDistribution = distribution.copy(distributedBy = user.userId)
            val result = aidRepository.recordDistribution(finalDistribution)
            
            result.fold(
                onSuccess = { _state.value = AidState.Success },
                onFailure = { _state.value = AidState.Error(it.message ?: "Failed to record distribution") }
            )
        }
    }
}

sealed class AidState {
    object Idle : AidState()
    object Loading : AidState()
    object Success : AidState()
    data class Error(val message: String) : AidState()
}
