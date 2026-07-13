package com.olivetrust.charity.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.olivetrust.charity.domain.model.DistributionEvent
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class EventListViewModel(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository
) : ScreenModel {
    val events: StateFlow<List<DistributionEvent>> = eventRepository.getEvents()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)
}
