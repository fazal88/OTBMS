package com.olivetrust.charity

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

class IOSDeviceInfo : DeviceInfo {
    override val id: String = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown"
    override val model: String = UIDevice.currentDevice.model
}

actual fun getDeviceInfo(): DeviceInfo = IOSDeviceInfo()