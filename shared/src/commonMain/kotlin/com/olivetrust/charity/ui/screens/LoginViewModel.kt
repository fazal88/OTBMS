package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.DeviceInfo
import com.olivetrust.charity.data.util.HashUtil
import com.olivetrust.charity.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val deviceInfo: DeviceInfo
) : ScreenModel {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    init {
        screenModelScope.launch {
            if (authRepository.tryAutoLogin()) {
                _state.value = LoginState.Success
            }
        }
    }

    fun login(username: String, password: String) {
        screenModelScope.launch {
            _state.value = LoginState.Loading
            val result = authRepository.login(
                username = username,
                passwordHash = HashUtil.hashPassword(password),
                deviceId = deviceInfo.id,
                deviceModel = deviceInfo.model
            )
            
            result.fold(
                onSuccess = { _state.value = LoginState.Success },
                onFailure = { _state.value = LoginState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
