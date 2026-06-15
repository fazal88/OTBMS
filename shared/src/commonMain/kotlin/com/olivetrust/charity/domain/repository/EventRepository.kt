package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.DistributionEvent
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<List<DistributionEvent>>
    fun getEventById(id: String): Flow<DistributionEvent?>
    suspend fun createEvent(event: DistributionEvent): Result<String>
    suspend fun updateEvent(event: DistributionEvent): Result<Unit>
    suspend fun addInvitee(eventId: String, beneficiaryId: String): Result<Unit>
}
