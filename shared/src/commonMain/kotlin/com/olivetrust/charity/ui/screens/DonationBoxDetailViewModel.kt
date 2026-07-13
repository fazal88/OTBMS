package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.Location
import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.DonationBoxRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DonationBoxDetailViewModel(
    private val boxId: String,
    private val boxRepository: DonationBoxRepository,
    private val authRepository: AuthRepository,
    private val auditRepository: com.olivetrust.charity.domain.repository.AuditRepository
) : ScreenModel {

    val currentUser = authRepository.currentUser.stateIn(
        screenModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val box = boxRepository.getDonationBoxById(boxId).stateIn(
        screenModelScope,
        SharingStarted.Eagerly,
        null
    )

    val collections = boxRepository.getCollectionsByBoxId(boxId).stateIn(
        screenModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val issues = boxRepository.getIssuesByBoxId(boxId).stateIn(
        screenModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val auditLogs = auditRepository.getLogsByEntity("DONATION_BOX", boxId).stateIn(
        screenModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess = _updateSuccess.asStateFlow()

    private val _selectedLocation = MutableStateFlow<Location?>(null)
    val selectedLocation = _selectedLocation.asStateFlow()

    init {
        screenModelScope.launch {
            box.collect { b ->
                if (b != null && _selectedLocation.value == null) {
                    _selectedLocation.value = Location(b.latitude, b.longitude)
                }
            }
        }
    }

    fun updateLocation(location: Location) {
        _selectedLocation.value = location
    }

    fun requestCurrentLocation() {
        val locationService = com.olivetrust.charity.getLocationService()
        screenModelScope.launch {
            try {
                val location = locationService.getCurrentLocation()
                location?.let {
                    _selectedLocation.value = it
                }
            } catch (e: Exception) {
                _error.value = "Failed to get current location: ${e.message}"
            }
        }
    }

    fun submitUpdate(box: DonationBox) {
        screenModelScope.launch {
            _isProcessing.value = true
            _updateSuccess.value = false
            
            val location = _selectedLocation.value
            val finalBox = if (location != null) {
                box.copy(latitude = location.latitude, longitude = location.longitude)
            } else {
                box
            }
            
            boxRepository.updateDonationBox(finalBox)
                .onSuccess { 
                    _updateSuccess.value = true
                }
                .onFailure { 
                    _error.value = it.message 
                }
            _isProcessing.value = false
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    fun approveBox() {
        val user = currentUser.value ?: return
        screenModelScope.launch {
            _isProcessing.value = true
            boxRepository.approveDonationBox(boxId, user.userId)
                .onFailure { _error.value = it.message }
            _isProcessing.value = false
        }
    }

    fun rejectBox(reason: String) {
        val user = currentUser.value ?: return
        screenModelScope.launch {
            _isProcessing.value = true
            boxRepository.rejectDonationBox(boxId, user.userId, reason)
                .onFailure { _error.value = it.message }
            _isProcessing.value = false
        }
    }

    fun approveIssue(issueId: String, newStatus: DonationBoxStatus, notes: String) {
        val user = currentUser.value ?: return
        screenModelScope.launch {
            _isProcessing.value = true
            boxRepository.approveIssue(issueId, boxId, user.userId, newStatus, notes)
                .onFailure { _error.value = it.message }
            _isProcessing.value = false
        }
    }

    fun rejectIssue(issueId: String, notes: String) {
        val user = currentUser.value ?: return
        screenModelScope.launch {
            _isProcessing.value = true
            boxRepository.rejectIssue(issueId, user.userId, notes)
                .onFailure { _error.value = it.message }
            _isProcessing.value = false
        }
    }

    fun confirmCollectionReceived(collectionId: String) {
        val user = currentUser.value ?: return
        screenModelScope.launch {
            _isProcessing.value = true
            boxRepository.confirmCollectionReceived(collectionId, user.userId)
                .onFailure { _error.value = it.message }
            _isProcessing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
