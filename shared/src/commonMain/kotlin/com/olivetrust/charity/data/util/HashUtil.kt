package com.olivetrust.charity.data.util

import org.kotlincrypto.hash.sha2.SHA256

object HashUtil {
    fun hashPassword(password: String): String {
        val bytes = password.encodeToByteArray()
        val digest = SHA256().digest(bytes)
        return digest.toHexString()
    }

    private fun ByteArray.toHexString(): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(size * 2)
        for (byte in this) {
            val i = byte.toInt() and 0xff
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }
}
