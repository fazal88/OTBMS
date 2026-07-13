package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.LocationService
import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.DistributionEvent
import com.olivetrust.charity.domain.repository.AidRepository
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.EventRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventAidDistributionViewModel(
    private val eventId: String,
    private val beneficiaryId: String,
    private val authRepository: AuthRepository,
    private val aidRepository: AidRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val eventRepository: EventRepository,
    private val locationService: LocationService
) : ScreenModel {

    private val _state = MutableStateFlow<AidState>(AidState.Idle)
    val state: StateFlow<AidState> = _state

    val event: StateFlow<DistributionEvent?> = eventRepository.getEventById(eventId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    val beneficiary: StateFlow<Beneficiary?> = beneficiaryRepository.getBeneficiaryById(beneficiaryId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun recordDistribution(distribution: AidDistribution) {
        screenModelScope.launch {
            _state.value = AidState.Loading
            val user = authRepository.currentUser.first()
            if (user == null) {
                _state.value = AidState.Error("User not authenticated")
                return@launch
            }

            val location = locationService.getCurrentLocation()

            val finalDistribution = distribution.copy(
                distributedBy = user.fullName,
                distributionLocationLat = location?.latitude ?: 0.0,
                distributionLocationLng = location?.longitude ?: 0.0,
                eventId = eventId
            )

            // Add as invitee if not already (for uninvited cases)
            eventRepository.addInvitee(eventId, beneficiaryId)

            val result = aidRepository.recordDistribution(finalDistribution)
            
            result.fold(
                onSuccess = { _state.value = AidState.Success },
                onFailure = { _state.value = AidState.Error(it.message ?: "Failed to record distribution") }
            )
        }
    }
}
