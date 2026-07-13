package com.olivetrust.charity

import platform.UIKit.UIDevice
import platform.UIKit.UIApplication
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.cinterop.*

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
    private var currentDelegate: CLLocationManagerDelegateProtocol? = null

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val locationManager = CLLocationManager()
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val locations = didUpdateLocations
                val location = locations.lastOrNull() as? CLLocation
                if (location != null) {
                    manager.stopUpdatingLocation()
                    if (continuation.isActive) {
                        val coordinate = location.coordinate
                        continuation.resume(Location(coordinate.useContents { latitude }, coordinate.useContents { longitude }))
                    }
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                when (didChangeAuthorizationStatus) {
                    kCLAuthorizationStatusAuthorizedWhenInUse,
                    kCLAuthorizationStatusAuthorizedAlways -> {
                        manager.startUpdatingLocation()
                    }
                    kCLAuthorizationStatusDenied,
                    kCLAuthorizationStatusRestricted -> {
                        if (continuation.isActive) continuation.resume(null)
                    }
                    else -> {}
                }
            }
        }

        currentDelegate = delegate
        locationManager.delegate = delegate

        val status = CLLocationManager.authorizationStatus()
        when (status) {
            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestWhenInUseAuthorization()
            }
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                locationManager.startUpdatingLocation()
            }
            else -> {
                if (continuation.isActive) continuation.resume(null)
            }
        }

        continuation.invokeOnCancellation {
            locationManager.stopUpdatingLocation()
            locationManager.delegate = null
            currentDelegate = null
        }
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

actual fun openMaps(latitude: Double, longitude: Double, label: String) {
    val urlString = if (label.isNotBlank()) {
        "https://maps.apple.com/?q=${label.replace(" ", "+")}&ll=$latitude,$longitude"
    } else {
        "https://maps.apple.com/?ll=$latitude,$longitude"
    }
    val url = NSURL.URLWithString(urlString)
    if (url != null) {
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}

actual suspend fun getPlatformFcmToken(): String? {
    // Delegates to the IosNotificationHelper bridge which waits for Swift's
    // MessagingDelegate to push the token, avoiding the timing race condition
    // that causes "no APNs token specified before fetching FCM token".
    return IosNotificationHelper.awaitFcmToken(timeoutSeconds = 15)
}

actual fun setScreenshotProtection(enabled: Boolean) {
    // Note: True screenshot prevention is not natively supported on iOS in the same way as Android's FLAG_SECURE.
    // Common workarounds include overlaying the screen when backgrounded or using a hidden UITextField.
    println("IOS_SCREENSHOT_PROTECTION: ${if (enabled) "Enabled" else "Disabled"} (Not fully implemented on iOS)")
}

