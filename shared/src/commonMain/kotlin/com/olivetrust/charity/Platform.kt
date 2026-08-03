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

expect fun openUrl(url: String)

expect fun shareUrl(url: String, title: String = "")

/**
 * Platform-specific FCM token retrieval.
 * - Android: calls Firebase Messaging directly (token is available immediately)
 * - iOS: waits for the token to be pushed from Swift's MessagingDelegate
 *   via IosNotificationHelper, avoiding the "no APNs token" race condition.
 */
expect suspend fun getPlatformFcmToken(): String?

expect fun setScreenshotProtection(enabled: Boolean)

interface FilePicker {
    suspend fun pickImage(): ByteArray?
    suspend fun pickImageOrPdf(): ByteArray?
    suspend fun takePhoto(): ByteArray?
}

expect fun getFilePicker(): FilePicker

interface AudioRecorder {
    fun startRecording()
    fun stopRecording(): ByteArray?
    fun isRecording(): Boolean
}

expect fun getAudioRecorder(): AudioRecorder

interface AudioPlayer {
    fun play(url: String)
    fun play(data: ByteArray)
    fun pause()
    fun resume()
    fun stop()
    fun isPlaying(): Boolean
    fun setCompletionListener(callback: () -> Unit)
}

expect fun getAudioPlayer(): AudioPlayer

