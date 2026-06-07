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
