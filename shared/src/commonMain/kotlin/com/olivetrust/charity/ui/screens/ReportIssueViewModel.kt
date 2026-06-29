package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.DonationBoxRepository
import com.olivetrust.charity.LocationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ReportIssueViewModel(
    private val boxId: String,
    private val boxRepository: DonationBoxRepository,
    private val authRepository: AuthRepository,
    private val locationService: LocationService
) : ScreenModel {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success = _success.asStateFlow()

    fun reportIssue(type: IssueType, description: String) {
        screenModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            
            try {
                val user = authRepository.currentUser.first()
                if (user == null) {
                    _error.value = "User not authenticated"
                    _isSubmitting.value = false
                    return@launch
                }
                
                val location = locationService.getCurrentLocation()
                val now = Clock.System.now().toEpochMilliseconds()
                val issue = DonationBoxIssue(
                    issueId = "ISSUE_$now",
                    donationBoxId = boxId,
                    reportType = type,
                    description = description,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    collectorId = user.userId
                )
                
                boxRepository.reportIssue(issue)
                    .onSuccess { _success.value = true }
                    .onFailure { _error.value = it.message }
            } catch (e: Exception) {
                _error.value = "Failed to report issue: ${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
