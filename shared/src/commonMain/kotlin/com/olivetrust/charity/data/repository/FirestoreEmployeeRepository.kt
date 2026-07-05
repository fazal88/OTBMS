package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.AuditLog
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.UserStatus
import com.olivetrust.charity.domain.repository.AuditRepository
import com.olivetrust.charity.domain.repository.EmployeeRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class FirestoreEmployeeRepository(
    private val auditRepository: AuditRepository
) : EmployeeRepository {
    private val firestore by lazy { Firebase.firestore }
    private val collection by lazy { firestore.collection("users") }

    override fun getEmployees(): Flow<List<User>> {
        return collection.snapshots().map { snapshot ->
            snapshot.documents.map { it.data(User.serializer()) }
                .filter { it.role != com.olivetrust.charity.domain.model.UserRole.BENEFICIARY }
        }
    }

    override suspend fun createEmployee(user: User, passwordHash: String): Result<String> {
        return try {
            val userWithPassword = user.copy(passwordHash = passwordHash)
            collection.document(user.userId).set(User.serializer(), userWithPassword)
            val now = Clock.System.now().toEpochMilliseconds()
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = "SYSTEM", 
                role = com.olivetrust.charity.domain.model.UserRole.APPROVER,
                actionType = "CREATE_EMPLOYEE",
                entityType = "USER",
                entityId = user.userId,
                timestamp = now,
                deviceId = ""
            ))
            Result.success(user.userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEmployee(user: User): Result<Unit> {
        return try {
            collection.document(user.userId).set(User.serializer(), user)
            val now = Clock.System.now().toEpochMilliseconds()
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = "SYSTEM",
                role = com.olivetrust.charity.domain.model.UserRole.APPROVER,
                actionType = "UPDATE_EMPLOYEE",
                entityType = "USER",
                entityId = user.userId,
                timestamp = now,
                deviceId = ""
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEmployee(userId: String): Result<Unit> {
        return try {
            collection.document(userId).delete()
            val now = Clock.System.now().toEpochMilliseconds()
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = "SYSTEM",
                role = com.olivetrust.charity.domain.model.UserRole.APPROVER,
                actionType = "DELETE_EMPLOYEE",
                entityType = "USER",
                entityId = userId,
                timestamp = now,
                deviceId = ""
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEmployeeStatus(userId: String, status: UserStatus): Result<Unit> {
        return try {
            val doc = collection.document(userId).get()
            val user = doc.data(User.serializer())
            
            val updated = if (status == UserStatus.DISABLED) {
                user.copy(
                    status = status,
                    deviceId = null,
                    deviceApproved = false
                )
            } else {
                user.copy(status = status)
            }

            collection.document(userId).set(User.serializer(), updated)
            val now = Clock.System.now().toEpochMilliseconds()
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = "SYSTEM",
                role = com.olivetrust.charity.domain.model.UserRole.APPROVER,
                actionType = "UPDATE_EMPLOYEE_STATUS",
                entityType = "USER",
                entityId = userId,
                timestamp = now,
                deviceId = "",
                oldValue = user.status.name,
                newValue = status.name
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveDevice(userId: String, deviceId: String, approverId: String): Result<Unit> {
        return try {
            val doc = collection.document(userId).get()
            val user = doc.data(User.serializer())
            val updated = user.copy(
                deviceId = deviceId, 
                deviceApproved = true
            )
            collection.document(userId).set(User.serializer(), updated)
            val now = Clock.System.now().toEpochMilliseconds()
            auditRepository.logAction(AuditLog(
                auditId = "A_$now",
                userId = approverId,
                role = com.olivetrust.charity.domain.model.UserRole.APPROVER,
                actionType = "APPROVE_DEVICE",
                entityType = "USER",
                entityId = userId,
                timestamp = now,
                deviceId = deviceId
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
