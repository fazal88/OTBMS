package com.olivetrust.charity.domain.repository

interface StorageRepository {
    suspend fun uploadFile(path: String, data: ByteArray, fileName: String): Result<String>
    suspend fun uploadPhoto(beneficiaryId: String, data: ByteArray): Result<String>
}
