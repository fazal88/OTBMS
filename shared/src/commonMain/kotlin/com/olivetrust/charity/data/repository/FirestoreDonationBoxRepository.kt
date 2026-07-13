package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.domain.model.SystemTopics
import com.olivetrust.charity.domain.repository.AuditRepository
import com.olivetrust.charity.domain.repository.DonationBoxRepository
import com.olivetrust.charity.domain.repository.NotificationRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlin.time.Clock

class FirestoreDonationBoxRepository(
    private val auditRepository: AuditRepository,
    private val notificationRepository: NotificationRepository
) : DonationBoxRepository {
    private val firestore by lazy { Firebase.firestore }
    private val boxCollection by lazy { firestore.collection("donation_boxes") }
    private val collectionsCollection by lazy { firestore.collection("collections") }
    private val issuesCollection by lazy { firestore.collection("donation_box_issues") }

    override fun getDonationBoxes(): Flow<List<DonationBox>> {
        return boxCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull {
                try {
                    it.data(DonationBox.serializer())
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override fun getDonationBoxById(id: String): Flow<DonationBox?> {
        return boxCollection.document(id).snapshots().map {
            try {
                if (it.exists) it.data(DonationBox.serializer()) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun getCollectionsByBoxId(boxId: String): Flow<List<DonationCollection>> {
        return collectionsCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull {
                try {
                    val coll = it.data(DonationCollection.serializer())
                    if (coll.donationBoxId == boxId) coll else null
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.timestamp }
        }
    }

    override fun getAllCollections(): Flow<List<DonationCollection>> {
        return collectionsCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull {
                try {
                    it.data(DonationCollection.serializer())
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.timestamp }
        }
    }

    override fun getIssuesByBoxId(boxId: String): Flow<List<DonationBoxIssue>> {
        return issuesCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull {
                try {
                    val issue = it.data(DonationBoxIssue.serializer())
                    if (issue.donationBoxId == boxId) issue else null
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.timestamp }
        }
    }

    override fun getAllIssues(): Flow<List<DonationBoxIssue>> {
        return issuesCollection.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull {
                try {
                    it.data(DonationBoxIssue.serializer())
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.timestamp }
        }
    }

    private suspend fun log(entityId: String, entityType: String, action: String, userId: String, role: UserRole = UserRole.COLLECTOR) {
        val now = Clock.System.now().toEpochMilliseconds()
        auditRepository.logAction(AuditLog(
            auditId = "AUDIT_$now",
            userId = userId,
            role = role,
            actionType = action,
            entityType = entityType,
            entityId = entityId,
            timestamp = now
        ))
    }

    override suspend fun createDonationBox(box: DonationBox): Result<String> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val finalBox = box.copy(
                installationDate = now,
                lastUpdated = now,
                status = DonationBoxStatus.PENDING_APPROVAL
            )
            boxCollection.document(finalBox.id).set(DonationBox.serializer(), finalBox)
            log(finalBox.id, "DONATION_BOX", "CREATE", finalBox.installedBy)
            Result.success(finalBox.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDonationBox(box: DonationBox): Result<Unit> {
        if (box.id.isBlank()) return Result.failure(Exception("Donation Box ID cannot be blank"))
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val updated = box.copy(lastUpdated = now)
            boxCollection.document(box.id).set(DonationBox.serializer(), updated)
            
            log(box.id, "DONATION_BOX", "UPDATE", "SYSTEM", UserRole.APPROVER)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveDonationBox(id: String, approverId: String): Result<Unit> {
        return try {
            val doc = boxCollection.document(id).get()
            val box = doc.data(DonationBox.serializer())
            val now = Clock.System.now().toEpochMilliseconds()
            val updated = box.copy(
                status = DonationBoxStatus.ACTIVE,
                lastUpdated = now
            )
            boxCollection.document(id).set(DonationBox.serializer(), updated)
            log(id, "DONATION_BOX", "APPROVE", approverId, UserRole.APPROVER)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectDonationBox(id: String, rejectedBy: String, reason: String): Result<Unit> {
        return try {
            val doc = boxCollection.document(id).get()
            val box = doc.data(DonationBox.serializer())
            val now = Clock.System.now().toEpochMilliseconds()
            val updated = box.copy(
                status = DonationBoxStatus.INACTIVE,
                rejectionReason = reason,
                lastUpdated = now
            )
            boxCollection.document(id).set(DonationBox.serializer(), updated)
            log(id, "DONATION_BOX", "REJECT", rejectedBy, UserRole.APPROVER)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordCollection(collection: DonationCollection): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val finalCollection = collection.copy(timestamp = now)
            collectionsCollection.document(finalCollection.collectionId).set(DonationCollection.serializer(), finalCollection)
            
            // Update box metadata
            val boxDoc = boxCollection.document(finalCollection.donationBoxId).get()
            val box = boxDoc.data(DonationBox.serializer())
            val updatedBox = box.copy(
                lastCollectionDate = now,
                lastCollectedAmount = finalCollection.amountCollected,
                lastUpdated = now
            )
            boxCollection.document(updatedBox.id).set(DonationBox.serializer(), updatedBox)
            
            notificationRepository.sendNotification(
                topicName = SystemTopics.COLLECTION,
                title = "Donation Collected",
                body = "₹${finalCollection.amountCollected} collected from box at ${updatedBox.address}."
            )

            log(finalCollection.collectionId, "COLLECTION", "RECORD", finalCollection.collectorId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reportIssue(issue: DonationBoxIssue): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val finalIssue = issue.copy(timestamp = now, status = IssueStatus.PENDING_REVIEW)
            issuesCollection.document(finalIssue.issueId).set(DonationBoxIssue.serializer(), finalIssue)
            
            // Update box status based on issue type
            val boxDoc = boxCollection.document(issue.donationBoxId).get()
            val box = boxDoc.data(DonationBox.serializer())
            val newStatus = when (issue.reportType) {
                IssueType.EDIT_REQUIRED -> DonationBoxStatus.PENDING_APPROVAL
                IssueType.INACTIVE -> DonationBoxStatus.INACTIVE
            }
            
            if (newStatus != box.status) {
                val updatedBox = box.copy(status = newStatus, lastUpdated = now)
                boxCollection.document(box.id).set(DonationBox.serializer(), updatedBox)
            }

            log(finalIssue.issueId, "ISSUE", "REPORT", finalIssue.collectorId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveIssue(issueId: String, boxId: String, reviewerId: String, newStatus: DonationBoxStatus, notes: String): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            
            // Update issue status
            val issueDoc = issuesCollection.document(issueId).get()
            val issue = issueDoc.data(DonationBoxIssue.serializer())
            val updatedIssue = issue.copy(
                status = IssueStatus.APPROVED,
                reviewNotes = notes,
                reviewedBy = reviewerId,
                reviewTimestamp = now
            )
            issuesCollection.document(issueId).set(DonationBoxIssue.serializer(), updatedIssue)
            
            // Update box status
            val boxDoc = boxCollection.document(boxId).get()
            val box = boxDoc.data(DonationBox.serializer())
            val updatedBox = box.copy(
                status = newStatus,
                lastUpdated = now
            )
            boxCollection.document(boxId).set(DonationBox.serializer(), updatedBox)
            
            log(issueId, "ISSUE", "APPROVE", reviewerId, UserRole.APPROVER)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectIssue(issueId: String, reviewerId: String, notes: String): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val issueDoc = issuesCollection.document(issueId).get()
            val issue = issueDoc.data(DonationBoxIssue.serializer())
            val updatedIssue = issue.copy(
                status = IssueStatus.REJECTED,
                reviewNotes = notes,
                reviewedBy = reviewerId,
                reviewTimestamp = now
            )
            issuesCollection.document(issueId).set(DonationBoxIssue.serializer(), updatedIssue)
            log(issueId, "ISSUE", "REJECT", reviewerId, UserRole.APPROVER)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
