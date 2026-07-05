package com.olivetrust.charity

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

interface DeviceInfo {
    val id: String
    val model: String
}

expect fun getDeviceInfo(): DeviceInfo

data class Location(val latitude: Double, val longitude: Double)

interface LocationService {
    suspend fun getCurrentLocation(): Location?
}

expect fun getLocationService(): LocationService

expect val isDebug: Boolean

expect fun sendSms(phoneNumber: String, message: String)

expect fun openMaps(latitude: Double, longitude: Double, label: String = "")

/**
 * Platform-specific FCM token retrieval.
 * - Android: calls Firebase Messaging directly (token is available immediately)
 * - iOS: waits for the token to be pushed from Swift's MessagingDelegate
 *   via IosNotificationHelper, avoiding the "no APNs token" race condition.
 */
expect suspend fun getPlatformFcmToken(): String?

