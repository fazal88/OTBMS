package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import com.olivetrust.charity.util.CommonSerializable
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlin.time.Clock

enum class SortOrder : CommonSerializable {
    NAME_ASC, NAME_DESC, 
    DATE_ADDED_ASC, DATE_ADDED_DESC, 
    DATE_UPDATED_ASC, DATE_UPDATED_DESC
}

data class BeneficiaryFilters(
    val status: BeneficiaryStatus? = null,
    val areaCode: String? = null,
    val natureOfAid: String? = null,
    val minPackets: Int? = null,
    val maxPackets: Int? = null,
    val reasonForAid: String? = null,
    val natureOfAddress: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val month: Int? = null,
    val year: Int? = null,
    val visitDue: Boolean = false
) : CommonSerializable

class BeneficiaryListViewModel(
    private val beneficiaryRepository: BeneficiaryRepository,
    private val authRepository: AuthRepository
) : ScreenModel {

    val currentUser = authRepository.currentUser.stateIn(
        screenModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filters = MutableStateFlow(BeneficiaryFilters())
    val filters = _filters.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val allBeneficiariesFlow = beneficiaryRepository.getBeneficiaries()
        .onEach { _error.value = null }
        .catch { _error.value = "Failed to load beneficiaries: ${it.message}" }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = allBeneficiariesFlow
        .map { it.size }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val beneficiaries: StateFlow<List<Beneficiary>> = combine(
        allBeneficiariesFlow,
        _searchQuery,
        _sortOrder,
        _filters
    ) { allBeneficiaries, query, sort, filter ->
        allBeneficiaries
            .asSequence()
            .filter { b ->
                applySearch(b, query) && applyFilters(b, filter)
            }
            .sortedWith { a, b ->
                applySort(a, b, sort)
            }
            .toList()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applySearch(b: Beneficiary, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        
        return b.headName.lowercase().contains(q) ||
               b.phoneNumber.contains(q) ||
               b.address.lowercase().contains(q) ||
               b.headOccupation.lowercase().contains(q) ||
               (b.approvalNotes?.lowercase()?.contains(q) == true) ||
               (b.rejectionReason?.lowercase()?.contains(q) == true) ||
               (b.editRequestNotes?.lowercase()?.contains(q) == true) ||
               b.familyMembers.any { 
                   it.name.lowercase().contains(q) || it.occupation.lowercase().contains(q) 
               }
    }

    private fun applyFilters(b: Beneficiary, f: BeneficiaryFilters): Boolean {
        if (f.status != null && b.status != f.status) return false
        
        if (!f.areaCode.isNullOrBlank()) {
            if (!b.areaCode.lowercase().trim().contains(f.areaCode.lowercase().trim())) return false
        }
        
        if (!f.natureOfAid.isNullOrBlank()) {
            val nature = b.natureOfAid?.lowercase()?.trim() ?: ""
            if (!nature.contains(f.natureOfAid.lowercase().trim())) return false
        }
        
        if (f.minPackets != null && (b.packetCount ?: 0) < f.minPackets) return false
        if (f.maxPackets != null && (b.packetCount ?: 0) > f.maxPackets) return false
        
        if (!f.reasonForAid.isNullOrBlank()) {
            if (!b.reasonForAid.lowercase().contains(f.reasonForAid.lowercase().trim())) return false
        }
        
        if (!f.natureOfAddress.isNullOrBlank()) {
            val addrNature = b.natureOfAddress.lowercase().trim()
            if (!addrNature.contains(f.natureOfAddress.lowercase().trim())) return false
        }
        
        if (f.minAmount != null && (b.monetaryAidAmount ?: 0.0) < f.minAmount) return false
        if (f.maxAmount != null && (b.monetaryAidAmount ?: 0.0) > f.maxAmount) return false

        if (f.month != null || f.year != null) {
            val dateTime = Instant.fromEpochMilliseconds(b.onboardingDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            
            if (f.month != null && dateTime.month.number != f.month) return false
            if (f.year != null && dateTime.year != f.year) return false
        }

        if (f.visitDue) {
            if (b.status != BeneficiaryStatus.APPROVED) return false
            
            val lastActionDate = b.lastVisitDate ?: b.approvalDate ?: b.onboardingDate
            val now = Clock.System.now()
            val visitInstant = Instant.fromEpochMilliseconds(lastActionDate)
            
            val twoMonthsAgo = now.minus(2, DateTimeUnit.MONTH, TimeZone.currentSystemDefault())
            
            if (visitInstant > twoMonthsAgo) return false
        }
        
        return true
    }

    private fun applySort(a: Beneficiary, b: Beneficiary, sort: SortOrder): Int {
        return when (sort) {
            SortOrder.NAME_ASC -> a.headName.compareTo(b.headName)
            SortOrder.NAME_DESC -> b.headName.compareTo(a.headName)
            SortOrder.DATE_ADDED_ASC -> a.onboardingDate.compareTo(b.onboardingDate)
            SortOrder.DATE_ADDED_DESC -> b.onboardingDate.compareTo(a.onboardingDate)
            SortOrder.DATE_UPDATED_ASC -> a.lastUpdated.compareTo(b.lastUpdated)
            SortOrder.DATE_UPDATED_DESC -> b.lastUpdated.compareTo(a.lastUpdated)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun updateFilters(newFilters: BeneficiaryFilters) {
        _filters.value = newFilters
    }
    
    fun resetFilters() {
        _filters.value = BeneficiaryFilters()
    }

    fun clearError() {
        _error.value = null
    }
}
