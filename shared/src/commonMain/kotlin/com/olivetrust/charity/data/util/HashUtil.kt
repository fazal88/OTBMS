package com.olivetrust.charity.data.util

object HashUtil {
    fun hashPassword(password: String): String {
        // Simple mock hash for KMP without external crypto libs yet
        // In a real app, use a proper KMP crypto library
        return "hashed_${password}"
    }
}
