package com.olivetrust.charity

import android.os.Build
import android.telephony.SmsManager
import android.content.Intent
import android.net.Uri

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
    val context = ContextHolder.get() ?: return
    try {
        val smsManager = context.getSystemService(SmsManager::class.java)
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        println("ANDROID_SMS: Sent background SMS to $phoneNumber")
    } catch (e: Exception) {
        println("ANDROID_SMS_ERROR: ${e.message}. Falling back to intent.")
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (inner: Exception) {
            println("ANDROID_SMS_FALLBACK_ERROR: ${inner.message}")
        }
    }
}
