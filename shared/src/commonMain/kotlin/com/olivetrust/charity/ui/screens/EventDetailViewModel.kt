package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.DistributionEvent
import com.olivetrust.charity.domain.repository.AidRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.EventRepository
import com.olivetrust.charity.sendSms
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class InviteeStatus(
    val beneficiary: Beneficiary,
    val hasReceivedAid: Boolean = false
)

class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val aidRepository: AidRepository
) : ScreenModel {

    val event: StateFlow<DistributionEvent?> = eventRepository.getEventById(eventId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val eventDistributions: StateFlow<List<AidDistribution>> = aidRepository.getDistributions()
        .map { list -> list.filter { it.eventId == eventId } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // For invitee list search
    private val _inviteeSearchQuery = MutableStateFlow("")
    val inviteeSearchQuery: StateFlow<String> = _inviteeSearchQuery

    val invitees: StateFlow<List<InviteeStatus>> = combine(
        event,
        beneficiaryRepository.getBeneficiaries(),
        eventDistributions,
        _inviteeSearchQuery
    ) { event, beneficiaries, distributions, query ->
        if (event == null) return@combine emptyList()
        
        val aidedBeneficiaryIds = distributions.map { it.beneficiaryId }.toSet()
        
        val allInvitees = event.inviteeIds.mapNotNull { id ->
            val bene = beneficiaries.find { it.id == id }
            bene?.let { InviteeStatus(it, aidedBeneficiaryIds.contains(id)) }
        }

        if (query.isBlank()) {
            allInvitees
        } else {
            allInvitees.filter { 
                it.beneficiary.headName.contains(query, ignoreCase = true) || 
                it.beneficiary.phoneNumber.contains(query)
            }
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

    fun onInviteeSearchQueryChange(query: String) { _inviteeSearchQuery.value = query }

    fun notifyAllInvitees() {
        val currentEvent = event.value ?: return
        val aidInfo = if (currentEvent.aidDescription.isNotBlank()) currentEvent.aidDescription else currentEvent.natureOfAid
        val message = "Dear Beneficiary, please come for aid distribution: ${currentEvent.name} on ${currentEvent.reason}. Aid: $aidInfo"
        
        screenModelScope.launch {
            invitees.value.forEach { 
                sendSms(it.beneficiary.phoneNumber, message)
            }
        }
    }
}
