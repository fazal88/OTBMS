package com.olivetrust.charity

import platform.UIKit.UIDevice
import kotlin.experimental.ExperimentalNativeApi

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

class IOSDeviceInfo : DeviceInfo {
    override val id: String = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown"
    override val model: String = UIDevice.currentDevice.model
}

actual fun getDeviceInfo(): DeviceInfo = IOSDeviceInfo()

class IOSLocationService : LocationService {
    override suspend fun getCurrentLocation(): Location? {
        // In a real app, use CoreLocation.
        return Location(31.5204, 74.3587)
    }
}

actual fun getLocationService(): LocationService = IOSLocationService()

@OptIn(ExperimentalNativeApi::class)
actual val isDebug: Boolean = kotlin.native.Platform.isDebugBinary

actual fun sendSms(phoneNumber: String, message: String) {
    val finalMessage = if (isDebug) "[TEST] $message" else message
    // In a real app, use MFMessageComposeViewController or tel: URL
    println("IOS_SMS: To $phoneNumber: $finalMessage")
}
