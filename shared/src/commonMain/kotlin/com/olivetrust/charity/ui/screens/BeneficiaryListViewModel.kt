package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BeneficiaryListViewModel(
    beneficiaryRepository: BeneficiaryRepository
) : ScreenModel {

    val beneficiaries: StateFlow<List<Beneficiary>> = beneficiaryRepository.getBeneficiaries()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
