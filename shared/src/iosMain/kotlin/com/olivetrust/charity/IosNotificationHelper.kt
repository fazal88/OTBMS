package com.olivetrust.charity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * iOS-only bridge that allows Swift's MessagingDelegate to push the FCM token
 * into the Kotlin layer as soon as Firebase delivers it.
 *
 * This avoids the "no APNs token specified before fetching FCM token" error that
 * occurs when messaging.getToken() is called before the APNs token has been set.
 *
 * Usage from Swift:
 *   IosNotificationHelper.shared.onFcmTokenReceived(token: fcmToken)
 */
object IosNotificationHelper {

    private val _fcmTokenFlow = MutableStateFlow<String?>(null)

    /**
     * Called from Swift's AppDelegate:
     *   func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?)
     */
    fun onFcmTokenReceived(token: String?) {
        println("IOS_BRIDGE: FCM token pushed from Swift delegate: $token")
        _fcmTokenFlow.value = token
    }

    /**
     * Suspends until the FCM token is available (pushed from Swift), or times out.
     * Returns null on timeout.
     */
    suspend fun awaitFcmToken(timeoutSeconds: Int = 15): String? {
        // If already received, return immediately
        _fcmTokenFlow.value?.let { return it }

        println("IOS_BRIDGE: Waiting for FCM token from Swift delegate (timeout: ${timeoutSeconds}s)...")
        return withTimeoutOrNull(timeoutSeconds.seconds) {
            _fcmTokenFlow.filterNotNull().first()
        }.also { token ->
            if (token == null) {
                println("IOS_BRIDGE: Timed out waiting for FCM token. Check APNs key in Firebase console.")
            } else {
                println("IOS_BRIDGE: FCM token received: $token")
            }
        }
    }
}
