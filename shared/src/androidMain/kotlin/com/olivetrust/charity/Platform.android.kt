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