package com.olivetrust.charity.domain.repository

import com.olivetrust.charity.domain.model.*
import kotlinx.coroutines.flow.Flow

interface DonationBoxRepository {
    fun getDonationBoxes(): Flow<List<DonationBox>>
    fun getDonationBoxById(id: String): Flow<DonationBox?>
    fun getCollectionsByBoxId(boxId: String): Flow<List<DonationCollection>>
    fun getAllCollections(): Flow<List<DonationCollection>>
    fun getIssuesByBoxId(boxId: String): Flow<List<DonationBoxIssue>>
    fun getAllIssues(): Flow<List<DonationBoxIssue>>

    suspend fun createDonationBox(box: DonationBox): Result<String>
    suspend fun updateDonationBox(box: DonationBox): Result<Unit>
    suspend fun approveDonationBox(id: String, approverId: String): Result<Unit>
    suspend fun rejectDonationBox(id: String, rejectedBy: String, reason: String): Result<Unit>
    suspend fun recordCollection(collection: DonationCollection): Result<Unit>
    suspend fun reportIssue(issue: DonationBoxIssue): Result<Unit>
    suspend fun approveIssue(issueId: String, boxId: String, reviewerId: String, newStatus: DonationBoxStatus, notes: String): Result<Unit>
    suspend fun rejectIssue(issueId: String, reviewerId: String, notes: String): Result<Unit>
}
