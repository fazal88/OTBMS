package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.ApprovalRecord
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.flow.*

enum class ApprovalSortOrder {
    DATE_DESC, DATE_ASC, NAME_ASC
}

data class ApprovalFilters(
    val approverName: String? = null,
    val natureOfAid: String? = null
)

class ApprovalListViewModel(
    private val beneficiaryRepository: BeneficiaryRepository
) : ScreenModel {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(ApprovalSortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filters = MutableStateFlow(ApprovalFilters())
    val filters = _filters.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val allApprovalsFlow = beneficiaryRepository.getApprovals()
        .onEach { _error.value = null }
        .catch { _error.value = "Failed to load approvals: ${it.message}" }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = allApprovalsFlow
        .map { it.size }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val approvals: StateFlow<List<ApprovalRecord>> = combine(
        allApprovalsFlow,
        _searchQuery,
        _sortOrder,
        _filters
    ) { allApprovals, query, sort, filter ->
        allApprovals
            .asSequence()
            .filter { a ->
                applySearch(a, query) && applyFilters(a, filter)
            }
            .sortedWith { a, b ->
                applySort(a, b, sort)
            }
            .toList()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applySearch(a: ApprovalRecord, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        return a.beneficiaryName.lowercase().contains(q) ||
               a.approverName.lowercase().contains(q) ||
               a.notes.lowercase().contains(q) ||
               a.natureOfAid.lowercase().contains(q)
    }

    private fun applyFilters(a: ApprovalRecord, f: ApprovalFilters): Boolean {
        if (!f.approverName.isNullOrBlank() && !a.approverName.contains(f.approverName, ignoreCase = true)) return false
        if (!f.natureOfAid.isNullOrBlank() && !a.natureOfAid.contains(f.natureOfAid, ignoreCase = true)) return false
        return true
    }

    private fun applySort(a: ApprovalRecord, b: ApprovalRecord, sort: ApprovalSortOrder): Int {
        return when (sort) {
            ApprovalSortOrder.DATE_DESC -> b.date.compareTo(a.date)
            ApprovalSortOrder.DATE_ASC -> a.date.compareTo(b.date)
            ApprovalSortOrder.NAME_ASC -> a.beneficiaryName.compareTo(b.beneficiaryName)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: ApprovalSortOrder) {
        _sortOrder.value = order
    }

    fun updateFilters(newFilters: ApprovalFilters) {
        _filters.value = newFilters
    }

    fun resetFilters() {
        _filters.value = ApprovalFilters()
        _searchQuery.value = ""
    }

    fun clearError() {
        _error.value = null
    }
}
