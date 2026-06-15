package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.LocationService
import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.DistributionEvent
import com.olivetrust.charity.domain.repository.AidRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.EventRepository
import com.olivetrust.charity.sendSms
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InviteeStatus(
    val beneficiary: Beneficiary,
    val hasReceivedAid: Boolean = false
)

class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val aidRepository: AidRepository,
    private val locationService: LocationService
) : ScreenModel {

    val event: StateFlow<DistributionEvent?> = eventRepository.getEventById(eventId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val eventDistributions: StateFlow<List<AidDistribution>> = aidRepository.getDistributions()
        .map { list -> list.filter { it.eventId == eventId } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invitees: StateFlow<List<InviteeStatus>> = combine(
        event,
        beneficiaryRepository.getBeneficiaries(),
        eventDistributions
    ) { event, beneficiaries, distributions ->
        if (event == null) return@combine emptyList()
        
        val aidedBeneficiaryIds = distributions.map { it.beneficiaryId }.toSet()
        
        event.inviteeIds.mapNotNull { id ->
            val bene = beneficiaries.find { it.id == id }
            bene?.let { InviteeStatus(it, aidedBeneficiaryIds.contains(id)) }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // For uninvited beneficiary search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResults: StateFlow<List<Beneficiary>> = combine(
        beneficiaryRepository.getBeneficiaries(),
        _searchQuery,
        event
    ) { beneficiaries, query, event ->
        if (query.isBlank() || event == null) return@combine emptyList()
        beneficiaries.filter { 
            it.status == com.olivetrust.charity.domain.model.BeneficiaryStatus.APPROVED &&
            !event.inviteeIds.contains(it.id) &&
            (it.headName.contains(query, ignoreCase = true) || it.phoneNumber.contains(query))
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun addUninvitedAndDistribute(beneficiary: Beneficiary, userId: String) {
        screenModelScope.launch {
            eventRepository.addInvitee(eventId, beneficiary.id).onSuccess {
                recordAid(beneficiary, userId)
                _searchQuery.value = ""
            }
        }
    }

    fun recordAid(beneficiary: Beneficiary, userId: String) {
        screenModelScope.launch {
            val currentEvent = event.value ?: return@launch
            val location = locationService.getCurrentLocation()
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            
            val distribution = AidDistribution(
                distributionId = "AID_${now}_${beneficiary.id}",
                date = now,
                beneficiaryId = beneficiary.id,
                beneficiaryName = beneficiary.headName,
                areaCode = beneficiary.areaCode,
                natureOfAid = currentEvent.natureOfAid,
                aidAmount = currentEvent.monetaryAidAmount ?: 0.0,
                packetCount = currentEvent.packetCount ?: 0,
                reason = currentEvent.reason,
                familyCount = beneficiary.numberOfDependants + 1,
                distributedBy = userId,
                distributionLocationLat = location?.latitude ?: 0.0,
                distributionLocationLng = location?.longitude ?: 0.0,
                eventId = eventId
            )
            
            aidRepository.recordDistribution(distribution)
        }
    }

    fun notifyAllInvitees() {
        val currentEvent = event.value ?: return
        val message = "Dear Beneficiary, please come for aid distribution: ${currentEvent.name} on ${currentEvent.reason}. Type: ${currentEvent.natureOfAid}"
        
        screenModelScope.launch {
            invitees.value.forEach { 
                sendSms(it.beneficiary.phoneNumber, message)
            }
        }
    }
}
