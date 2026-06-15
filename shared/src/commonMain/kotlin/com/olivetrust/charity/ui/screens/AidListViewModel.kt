package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.AidDistribution
import com.olivetrust.charity.domain.model.DistributionEvent
import com.olivetrust.charity.domain.repository.AidRepository
import com.olivetrust.charity.domain.repository.EventRepository
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

enum class AidSortOrder {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, NAME_ASC
}

data class AidFilters(
    val areaCode: String? = null,
    val natureOfAid: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val month: Int? = null,
    val year: Int? = null
)

data class AidDistributionWithEvent(
    val distribution: AidDistribution,
    val eventName: String? = null
)

class AidListViewModel(
    private val aidRepository: AidRepository,
    private val eventRepository: EventRepository
) : ScreenModel {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(AidSortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filters = MutableStateFlow(AidFilters())
    val filters = _filters.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val allAidFlow = aidRepository.getDistributions()
        .onEach { _error.value = null }
        .catch { _error.value = "Failed to load aid distributions: ${it.message}" }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allEventsFlow = eventRepository.getEvents()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = allAidFlow
        .map { it.size }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val distributions: StateFlow<List<AidDistributionWithEvent>> = combine(
        allAidFlow,
        allEventsFlow,
        _searchQuery,
        _sortOrder,
        _filters
    ) { allAid, allEvents, query, sort, filter ->
        allAid
            .asSequence()
            .filter { a ->
                applySearch(a, query) && applyFilters(a, filter)
            }
            .map { a ->
                val event = allEvents.find { it.id == a.eventId }
                AidDistributionWithEvent(a, event?.name)
            }
            .sortedWith { a, b ->
                applySort(a.distribution, b.distribution, sort)
            }
            .toList()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applySearch(a: AidDistribution, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        return a.beneficiaryName.lowercase().contains(q) ||
               a.receiverName.lowercase().contains(q) ||
               a.areaCode.lowercase().contains(q) ||
               a.natureOfAid.lowercase().contains(q) ||
               a.reason.lowercase().contains(q) ||
               a.distributedBy.lowercase().contains(q)
    }

    private fun applyFilters(a: AidDistribution, f: AidFilters): Boolean {
        if (!f.areaCode.isNullOrBlank() && !a.areaCode.contains(f.areaCode, ignoreCase = true)) return false
        if (!f.natureOfAid.isNullOrBlank() && !a.natureOfAid.contains(f.natureOfAid, ignoreCase = true)) return false
        if (f.minAmount != null && a.aidAmount < f.minAmount) return false
        if (f.maxAmount != null && a.aidAmount > f.maxAmount) return false

        if (f.month != null || f.year != null) {
            val dateTime = Instant.fromEpochMilliseconds(a.date)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            
            if (f.month != null && dateTime.month.number != f.month) return false
            if (f.year != null && dateTime.year != f.year) return false
        }

        return true
    }

    private fun applySort(a: AidDistribution, b: AidDistribution, sort: AidSortOrder): Int {
        return when (sort) {
            AidSortOrder.DATE_DESC -> b.date.compareTo(a.date)
            AidSortOrder.DATE_ASC -> a.date.compareTo(b.date)
            AidSortOrder.AMOUNT_DESC -> b.aidAmount.compareTo(a.aidAmount)
            AidSortOrder.NAME_ASC -> a.beneficiaryName.compareTo(b.beneficiaryName)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: AidSortOrder) {
        _sortOrder.value = order
    }

    fun updateFilters(newFilters: AidFilters) {
        _filters.value = newFilters
    }

    fun resetFilters() {
        _filters.value = AidFilters()
        _searchQuery.value = ""
    }

    fun clearError() {
        _error.value = null
    }
}
