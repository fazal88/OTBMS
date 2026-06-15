package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.AuditLog
import com.olivetrust.charity.domain.model.DistributionEvent
import com.olivetrust.charity.domain.model.UserRole
import com.olivetrust.charity.domain.repository.AuditRepository
import com.olivetrust.charity.domain.repository.EventRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreEventRepository(
    private val auditRepository: AuditRepository
) : EventRepository {
    private val firestore by lazy { Firebase.firestore }
    private val collection by lazy { firestore.collection("distributionEvents") }

    override fun getEvents(): Flow<List<DistributionEvent>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { 
                try {
                    it.data(DistributionEvent.serializer())
                } catch (e: Exception) {
                    println("FIRESTORE_ERROR: Failed to decode event ${it.id}: ${e.message}")
                    null
                }
            }
        }
    }

    override fun getEventById(id: String): Flow<DistributionEvent?> {
        return collection.document(id).snapshots().map { 
            try {
                if (it.exists) it.data(DistributionEvent.serializer()) else null 
            } catch (e: Exception) {
                println("FIRESTORE_ERROR: Failed to decode event $id: ${e.message}")
                null
            }
        }
    }

    override suspend fun createEvent(event: DistributionEvent): Result<String> {
        return try {
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val finalEvent = event.copy(createdAt = now)
            collection.document(finalEvent.id).set(DistributionEvent.serializer(), finalEvent)
            
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = finalEvent.createdBy,
                role = UserRole.EMPLOYEE, // Default to employee for now
                actionType = "CREATE_EVENT",
                entityType = "EVENT",
                entityId = finalEvent.id,
                timestamp = now,
                deviceId = ""
            ))
            Result.success(finalEvent.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: DistributionEvent): Result<Unit> {
        return try {
            collection.document(event.id).set(DistributionEvent.serializer(), event)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addInvitee(eventId: String, beneficiaryId: String): Result<Unit> {
        return try {
            val doc = collection.document(eventId).get()
            if (doc.exists) {
                val event = doc.data(DistributionEvent.serializer())
                if (!event.inviteeIds.contains(beneficiaryId)) {
                    val updated = event.copy(inviteeIds = event.inviteeIds + beneficiaryId)
                    collection.document(eventId).set(DistributionEvent.serializer(), updated)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
