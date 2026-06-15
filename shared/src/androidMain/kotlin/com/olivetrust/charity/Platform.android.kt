package com.olivetrust.charity

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

class AndroidDeviceInfo : DeviceInfo {
    override val id: String = Build.FINGERPRINT
    override val model: String = "${Build.MANUFACTURER} ${Build.MODEL}"
}

actual fun getDeviceInfo(): DeviceInfo = AndroidDeviceInfo()

class AndroidLocationService : LocationService {
    override suspend fun getCurrentLocation(): Location? {
        // In a real app, use FusedLocationProviderClient and check permissions.
        // For this demo, we'll return a sample location to show it's being recorded.
        return Location(31.5204, 74.3587) // Lahore coordinates
    }
}

actual fun getLocationService(): LocationService = AndroidLocationService()

actual fun sendSms(phoneNumber: String, message: String) {
    // In a real app, you'd use a Context to start an intent.
    // Since shared module doesn't easily have reference to MainActivity, 
    // we'd typically inject a context or use a message bridge.
    // For now, keeping it as a println but documented.
    println("ANDROID_SMS_INTENT_TRIGGERED: To $phoneNumber: $message")
}
