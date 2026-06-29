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

class RecordCollectionViewModel(
    private val boxId: String,
    private val boxRepository: DonationBoxRepository,
    private val authRepository: AuthRepository,
    private val locationService: LocationService
) : ScreenModel {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _success = MutableStateFlow<DonationCollection?>(null)
    val success = _success.asStateFlow()

    fun recordCollection(amount: Double, remarks: String?) {
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
                val collection = DonationCollection(
                    collectionId = "COLL_$now",
                    donationBoxId = boxId,
                    amountCollected = amount,
                    latitude = location?.latitude ?: 0.0,
                    longitude = location?.longitude ?: 0.0,
                    collectorId = user.userId,
                    collectorName = user.fullName,
                    remarks = remarks
                )
                
                boxRepository.recordCollection(collection)
                    .onSuccess { _success.value = collection }
                    .onFailure { _error.value = it.message }
            } catch (e: Exception) {
                _error.value = "Failed to record collection: ${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
