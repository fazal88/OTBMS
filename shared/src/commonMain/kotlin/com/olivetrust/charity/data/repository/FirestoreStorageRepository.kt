package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.repository.StorageRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage

expect fun toFirebaseData(data: ByteArray): dev.gitlive.firebase.storage.Data

class FirestoreStorageRepository : StorageRepository {
    private val storage by lazy { Firebase.storage }

    override suspend fun uploadFile(path: String, data: ByteArray, fileName: String): Result<String> {
        return try {
            val ref = storage.reference("$path/$fileName")
            ref.putData(toFirebaseData(data))
            val url = ref.getDownloadUrl()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPhoto(beneficiaryId: String, data: ByteArray): Result<String> {
        return uploadFile("beneficiaries/$beneficiaryId", data, "profile.jpg")
    }
}
