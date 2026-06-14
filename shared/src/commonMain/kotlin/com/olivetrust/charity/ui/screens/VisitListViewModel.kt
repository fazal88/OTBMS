package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.VerificationVisit
import com.olivetrust.charity.domain.model.VisitStatus
import com.olivetrust.charity.domain.repository.VisitRepository
import kotlinx.coroutines.flow.*

enum class VisitSortOrder {
    DATE_DESC, DATE_ASC, NAME_ASC
}

data class VisitFilters(
    val status: VisitStatus? = null,
    val areaCode: String? = null,
    val employeeName: String? = null
)

class VisitListViewModel(
    private val visitRepository: VisitRepository
) : ScreenModel {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(VisitSortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filters = MutableStateFlow(VisitFilters())
    val filters = _filters.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val allVisitsFlow = visitRepository.getVisits()
        .onEach { _error.value = null }
        .catch { _error.value = "Failed to load visits: ${it.message}" }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = allVisitsFlow
        .map { it.size }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val visits: StateFlow<List<VerificationVisit>> = combine(
        allVisitsFlow,
        _searchQuery,
        _sortOrder,
        _filters
    ) { allVisits, query, sort, filter ->
        allVisits
            .asSequence()
            .filter { v ->
                applySearch(v, query) && applyFilters(v, filter)
            }
            .sortedWith { a, b ->
                applySort(a, b, sort)
            }
            .toList()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applySearch(v: VerificationVisit, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        return v.beneficiaryName.lowercase().contains(q) ||
               v.employeeId.lowercase().contains(q) || // employeeId holds name in seeder
               v.areaCode.lowercase().contains(q) ||
               (v.reapprovalReason?.lowercase()?.contains(q) == true) ||
               (v.misuseReport?.description?.lowercase()?.contains(q) == true)
    }

    private fun applyFilters(v: VerificationVisit, f: VisitFilters): Boolean {
        if (f.status != null && v.visitStatus != f.status) return false
        if (!f.areaCode.isNullOrBlank() && !v.areaCode.contains(f.areaCode, ignoreCase = true)) return false
        if (!f.employeeName.isNullOrBlank() && !v.employeeId.contains(f.employeeName, ignoreCase = true)) return false
        return true
    }

    private fun applySort(a: VerificationVisit, b: VerificationVisit, sort: VisitSortOrder): Int {
        return when (sort) {
            VisitSortOrder.DATE_DESC -> b.date.compareTo(a.date)
            VisitSortOrder.DATE_ASC -> a.date.compareTo(b.date)
            VisitSortOrder.NAME_ASC -> a.beneficiaryName.compareTo(b.beneficiaryName)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: VisitSortOrder) {
        _sortOrder.value = order
    }

    fun updateFilters(newFilters: VisitFilters) {
        _filters.value = newFilters
    }

    fun resetFilters() {
        _filters.value = VisitFilters()
        _searchQuery.value = ""
    }

    fun clearError() {
        _error.value = null
    }
}
