package com.olivetrust.charity

import android.os.Build
import android.telephony.SmsManager
import android.content.Intent
import android.net.Uri
import android.content.pm.ApplicationInfo
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import android.annotation.SuppressLint

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
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? {
        val context = ContextHolder.get() ?: return null
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                Location(location.latitude, location.longitude)
            } else {
                // Fallback to sample for demo if no last location
                Location(31.5204, 74.3587)
            }
        } catch (e: Exception) {
            println("ANDROID_LOCATION_ERROR: ${e.message}")
            Location(31.5204, 74.3587)
        }
    }
}

actual fun getLocationService(): LocationService = AndroidLocationService()

actual val isDebug: Boolean
    get() = ContextHolder.get()?.let { (it.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 } ?: true

actual fun sendSms(phoneNumber: String, message: String) {
    val finalMessage = if (isDebug) "[TEST] $message" else message
    val context = ContextHolder.get() ?: return
    try {
        val smsManager = context.getSystemService(SmsManager::class.java)
        val parts = smsManager.divideMessage(finalMessage)
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        println("ANDROID_SMS: Sent background SMS to $phoneNumber")
    } catch (e: Exception) {
        println("ANDROID_SMS_ERROR: ${e.message}. Falling back to intent.")
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", finalMessage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (inner: Exception) {
            println("ANDROID_SMS_FALLBACK_ERROR: ${inner.message}")
        }
    }
}
