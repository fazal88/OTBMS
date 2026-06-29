package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.DonationBox
import com.olivetrust.charity.domain.model.DonationBoxStatus
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.DonationBoxRepository
import com.olivetrust.charity.domain.util.LocationUtil
import com.olivetrust.charity.Location
import com.olivetrust.charity.LocationService
import com.olivetrust.charity.util.CommonSerializable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

enum class BoxSortOrder : CommonSerializable {
    ID_ASC, ID_DESC,
    INSTALLATION_DATE_ASC, INSTALLATION_DATE_DESC,
    LAST_COLLECTION_DATE_ASC, LAST_COLLECTION_DATE_DESC,
    LAST_UPDATED_ASC, LAST_UPDATED_DESC,
    DISTANCE_ASC
}

data class DonationBoxFilters(
    val status: DonationBoxStatus? = null,
    val areaCode: String? = null,
    val collectorId: String? = null,
    val minInstallationDate: Long? = null,
    val maxInstallationDate: Long? = null,
    val minLastCollectionDate: Long? = null,
    val maxLastCollectionDate: Long? = null
) : CommonSerializable

class DonationBoxListViewModel(
    private val boxRepository: DonationBoxRepository,
    private val authRepository: AuthRepository,
    private val locationService: LocationService
) : ScreenModel {

    val currentUser = authRepository.currentUser.stateIn(
        screenModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(BoxSortOrder.INSTALLATION_DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filters = MutableStateFlow(DonationBoxFilters())
    val filters = _filters.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadCurrentLocation()
    }

    private fun loadCurrentLocation() {
        screenModelScope.launch {
            _currentLocation.value = locationService.getCurrentLocation()
        }
    }

    private val allBoxesFlow = boxRepository.getDonationBoxes()
        .onEach { _error.value = null }
        .catch { _error.value = "Failed to load donation boxes: ${it.message}" }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val boxes: StateFlow<List<DonationBox>> = combine(
        allBoxesFlow,
        _searchQuery,
        _sortOrder,
        _filters,
        _currentLocation
    ) { allBoxes, query, sort, filter, location ->
        allBoxes
            .asSequence()
            .filter { b ->
                applySearch(b, query) && applyFilters(b, filter)
            }
            .sortedWith { a, b ->
                applySort(a, b, sort, location)
            }
            .toList()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applySearch(b: DonationBox, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        
        return b.id.lowercase().contains(q) ||
               b.address.lowercase().contains(q) ||
               b.areaCode.lowercase().contains(q) ||
               b.personOfContact.lowercase().contains(q) ||
               b.contactNumber.contains(q) ||
               b.installedBy.lowercase().contains(q)
    }

    private fun applyFilters(b: DonationBox, f: DonationBoxFilters): Boolean {
        if (f.status != null && b.status != f.status) return false
        if (!f.areaCode.isNullOrBlank() && !b.areaCode.lowercase().contains(f.areaCode.lowercase())) return false
        if (!f.collectorId.isNullOrBlank() && b.installedBy != f.collectorId) return false
        
        if (f.minInstallationDate != null && b.installationDate < f.minInstallationDate) return false
        if (f.maxInstallationDate != null && b.installationDate > f.maxInstallationDate) return false
        
        if (f.minLastCollectionDate != null && (b.lastCollectionDate ?: 0) < f.minLastCollectionDate) return false
        if (f.maxLastCollectionDate != null && (b.lastCollectionDate ?: 0) > f.maxLastCollectionDate) return false
        
        return true
    }

    private fun applySort(a: DonationBox, b: DonationBox, sort: BoxSortOrder, location: Location?): Int {
        return when (sort) {
            BoxSortOrder.ID_ASC -> a.id.compareTo(b.id)
            BoxSortOrder.ID_DESC -> b.id.compareTo(a.id)
            BoxSortOrder.INSTALLATION_DATE_ASC -> a.installationDate.compareTo(b.installationDate)
            BoxSortOrder.INSTALLATION_DATE_DESC -> b.installationDate.compareTo(a.installationDate)
            BoxSortOrder.LAST_COLLECTION_DATE_ASC -> (a.lastCollectionDate ?: 0).compareTo(b.lastCollectionDate ?: 0)
            BoxSortOrder.LAST_COLLECTION_DATE_DESC -> (b.lastCollectionDate ?: 0).compareTo(a.lastCollectionDate ?: 0)
            BoxSortOrder.LAST_UPDATED_ASC -> a.lastUpdated.compareTo(b.lastUpdated)
            BoxSortOrder.LAST_UPDATED_DESC -> b.lastUpdated.compareTo(a.lastUpdated)
            BoxSortOrder.DISTANCE_ASC -> {
                if (location == null) 0
                else {
                    val distA = LocationUtil.calculateDistance(location.latitude, location.longitude, a.latitude, a.longitude)
                    val distB = LocationUtil.calculateDistance(location.latitude, location.longitude, b.latitude, b.longitude)
                    distA.compareTo(distB)
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: BoxSortOrder) {
        _sortOrder.value = order
    }

    fun updateFilters(newFilters: DonationBoxFilters) {
        _filters.value = newFilters
    }
    
    fun resetFilters() {
        _filters.value = DonationBoxFilters()
    }
}
