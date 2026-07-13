package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.Location
import com.olivetrust.charity.domain.model.DonationBox
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.DonationBoxRepository
import com.olivetrust.charity.LocationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock

class InstallDonationBoxViewModel(
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

    private val _selectedLocation = MutableStateFlow<Location?>(null)
    val selectedLocation = _selectedLocation.asStateFlow()

    init {
        requestCurrentLocation()
    }

    fun requestCurrentLocation() {
        screenModelScope.launch {
            try {
                val location = locationService.getCurrentLocation()
                _selectedLocation.value = location
            } catch (e: Exception) {
                _error.value = "Failed to get location: ${e.message}"
            }
        }
    }

    fun updateLocation(location: Location) {
        _selectedLocation.value = location
    }

    fun installBox(
        address: String,
        personOfContact: String,
        contactNumber: String,
        areaCode: String,
        remarks: String?
    ) {
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

                val location = _selectedLocation.value
                val now = Clock.System.now().toEpochMilliseconds()
                val box = DonationBox(
                    id = "DBX_$now",
                    address = address,
                    personOfContact = personOfContact,
                    contactNumber = contactNumber,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    areaCode = areaCode,
                    installedBy = user.userId,
                    remarks = remarks
                )
                
                boxRepository.createDonationBox(box)
                    .onSuccess { _success.value = true }
                    .onFailure { _error.value = it.message }
            } catch (e: Exception) {
                _error.value = "Failed to submit: ${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
