package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.DonationBoxRepository
import com.olivetrust.charity.util.CommonSerializable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

enum class CollectionSortOrder : CommonSerializable {
    DATE_DESC, DATE_ASC,
    AMOUNT_DESC, AMOUNT_ASC
}

data class DonationCollectionFilters(
    val status: CollectionStatus? = null,
    val collectorId: String? = null,
    val boxId: String? = null
) : CommonSerializable

class DonationCollectionListViewModel(
    private val boxRepository: DonationBoxRepository,
    private val authRepository: AuthRepository
) : ScreenModel {

    val currentUser = authRepository.currentUser.stateIn(
        screenModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(CollectionSortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filters = MutableStateFlow(DonationCollectionFilters())
    val filters = _filters.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    private val allCollectionsFlow = boxRepository.getAllCollections()
        .onEach { _error.value = null }
        .catch { _error.value = "Failed to load collections: ${it.message}" }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<DonationCollection>> = combine(
        allCollectionsFlow,
        _searchQuery,
        _sortOrder,
        _filters
    ) { all, query, sort, filter ->
        all.asSequence()
            .filter { applySearch(it, query) && applyFilters(it, filter) }
            .sortedWith { a, b -> applySort(a, b, sort) }
            .toList()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applySearch(c: DonationCollection, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        return c.collectorName.lowercase().contains(q) || 
               c.donationBoxId.lowercase().contains(q) ||
               (c.remarks?.lowercase()?.contains(q) ?: false)
    }

    private fun applyFilters(c: DonationCollection, f: DonationCollectionFilters): Boolean {
        if (f.status != null && c.status != f.status) return false
        if (f.collectorId != null && c.collectorId != f.collectorId) return false
        if (f.boxId != null && c.donationBoxId != f.boxId) return false
        return true
    }

    private fun applySort(a: DonationCollection, b: DonationCollection, sort: CollectionSortOrder): Int {
        return when (sort) {
            CollectionSortOrder.DATE_DESC -> b.timestamp.compareTo(a.timestamp)
            CollectionSortOrder.DATE_ASC -> a.timestamp.compareTo(b.timestamp)
            CollectionSortOrder.AMOUNT_DESC -> b.amountCollected.compareTo(a.amountCollected)
            CollectionSortOrder.AMOUNT_ASC -> a.amountCollected.compareTo(b.amountCollected)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: CollectionSortOrder) {
        _sortOrder.value = order
    }

    fun updateFilters(newFilters: DonationCollectionFilters) {
        _filters.value = newFilters
    }

    fun resetFilters() {
        _filters.value = DonationCollectionFilters()
    }

    fun confirmReceived(collectionId: String) {
        val user = currentUser.value ?: return
        screenModelScope.launch {
            _isProcessing.value = true
            boxRepository.confirmCollectionReceived(collectionId, user.userId)
                .onFailure { _error.value = it.message }
            _isProcessing.value = false
        }
    }

    fun toggleSelection(id: String) {
        _selectedIds.value = if (_selectedIds.value.contains(id)) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun confirmSelectedReceived() {
        val user = currentUser.value ?: return
        val ids = _selectedIds.value
        if (ids.isEmpty()) return

        screenModelScope.launch {
            _isProcessing.value = true
            var lastError: String? = null
            ids.forEach { id ->
                boxRepository.confirmCollectionReceived(id, user.userId)
                    .onFailure { lastError = it.message }
            }
            if (lastError != null) {
                _error.value = "Some collections failed: $lastError"
            }
            _selectedIds.value = emptySet()
            _isProcessing.value = false
        }
    }
}
