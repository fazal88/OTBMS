package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.AuditLog
import com.olivetrust.charity.domain.repository.AuditRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreAuditRepository : AuditRepository {
    private val firestore by lazy { Firebase.firestore }
    private val collection by lazy { firestore.collection("auditLogs") }

    override fun getLogs(): Flow<List<AuditLog>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.map { it.data(AuditLog.serializer()) }
        }
    }

    override fun getLogsByEntity(entityType: String, entityId: String): Flow<List<AuditLog>> {
        // GitLive syntax for where is sometimes tricky depending on version. 
        // Using a simpler approach for now to ensure compilation.
        return collection.snapshots().map { snapshot ->
            snapshot.documents.map { it.data(AuditLog.serializer()) }
                .filter { it.entityType == entityType && it.entityId == entityId }
        }
    }

    override suspend fun logAction(log: AuditLog): Result<Unit> {
        return try {
            collection.document(log.auditId).set(AuditLog.serializer(), log)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
