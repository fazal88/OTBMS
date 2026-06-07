package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.UserStatus
import com.olivetrust.charity.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmployeeManagementViewModel(
    private val employeeRepository: EmployeeRepository
) : ScreenModel {

    val employees: StateFlow<List<User>> = employeeRepository.getEmployees()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateStatus(userId: String, status: UserStatus) {
        screenModelScope.launch {
            employeeRepository.updateEmployeeStatus(userId, status)
        }
    }
}
