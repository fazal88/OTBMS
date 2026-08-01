package com.olivetrust.charity.data.repository

import dev.gitlive.firebase.storage.Data
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun toFirebaseData(data: ByteArray): Data {
    val nsData = data.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = data.size.toULong())
    }
    return Data(nsData)
}
