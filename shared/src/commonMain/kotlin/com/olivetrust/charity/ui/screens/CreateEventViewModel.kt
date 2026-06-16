package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.model.DistributionEvent
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class CreateEventViewModel(
    private val eventRepository: EventRepository,
    private val beneficiaryRepository: BeneficiaryRepository
) : ScreenModel {
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _reason = MutableStateFlow("")
    val reason: StateFlow<String> = _reason

    private val _natureOfAid = MutableStateFlow("Ration")
    val natureOfAid: StateFlow<String> = _natureOfAid

    private val _packetCount = MutableStateFlow<Int?>(null)
    val packetCount: StateFlow<Int?> = _packetCount

    private val _monetaryAmount = MutableStateFlow<Double?>(null)
    val monetaryAmount: StateFlow<Double?> = _monetaryAmount

    private val _areaCodeFilter = MutableStateFlow("")
    val areaCodeFilter: StateFlow<String> = _areaCodeFilter

    private val _selectedBeneficiaryIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBeneficiaryIds: StateFlow<Set<String>> = _selectedBeneficiaryIds

    val filteredBeneficiaries: StateFlow<List<Beneficiary>> = combine(
        beneficiaryRepository.getBeneficiaries(),
        _areaCodeFilter
    ) { beneficiaries, filter ->
        beneficiaries.filter { 
            it.status == BeneficiaryStatus.APPROVED && 
            (filter.isBlank() || it.areaCode.contains(filter, ignoreCase = true))
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onNameChange(value: String) { _name.value = value }
    fun onReasonChange(value: String) { _reason.value = value }
    fun onNatureOfAidChange(value: String) { _natureOfAid.value = value }
    fun onPacketCountChange(value: Int?) { _packetCount.value = value }
    fun onMonetaryAmountChange(value: Double?) { _monetaryAmount.value = value }
    fun onAreaCodeFilterChange(value: String) { _areaCodeFilter.value = value }

    fun toggleBeneficiarySelection(id: String) {
        val current = _selectedBeneficiaryIds.value
        if (current.contains(id)) {
            _selectedBeneficiaryIds.value = current - id
        } else {
            _selectedBeneficiaryIds.value = current + id
        }
    }

    fun selectAllFiltered() {
        _selectedBeneficiaryIds.value = _selectedBeneficiaryIds.value + filteredBeneficiaries.value.map { it.id }.toSet()
    }

    fun createEvent(userId: String, onSuccess: () -> Unit) {
        screenModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val event = DistributionEvent(
                id = "EVT_$now",
                name = _name.value,
                date = now,
                reason = _reason.value,
                natureOfAid = _natureOfAid.value,
                packetCount = _packetCount.value,
                monetaryAidAmount = _monetaryAmount.value,
                inviteeIds = _selectedBeneficiaryIds.value.toList(),
                createdBy = userId
            )
            val result = eventRepository.createEvent(event)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }
}
