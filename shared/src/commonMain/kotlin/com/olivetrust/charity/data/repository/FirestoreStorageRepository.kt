package com.olivetrust.charity.data.repository

import com.olivetrust.charity.domain.repository.StorageRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage

expect fun toFirebaseData(data: ByteArray): dev.gitlive.firebase.storage.Data

class FirestoreStorageRepository : StorageRepository {
    private val storage by lazy { Firebase.storage }

    override suspend fun uploadFile(path: String, data: ByteArray, fileName: String): Result<String> {
        return try {
            println("FIRESTORE_STORAGE: Uploading $fileName to $path (${data.size} bytes)")
            val ref = storage.reference("$path/$fileName")
            ref.putData(toFirebaseData(data))
            val url = ref.getDownloadUrl()
            println("FIRESTORE_STORAGE: Upload successful. URL: $url")
            Result.success(url)
        } catch (e: Exception) {
            println("FIRESTORE_STORAGE_ERROR: Failed to upload $fileName: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun uploadPhoto(beneficiaryId: String, data: ByteArray): Result<String> {
        return uploadFile("beneficiaries/$beneficiaryId", data, "profile.jpg")
    }
}
