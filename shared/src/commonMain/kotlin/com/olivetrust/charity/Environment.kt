package com.olivetrust.charity

enum class Environment {
    UAT,
    PRODUCTION
}

data class AppConfig(
    val environment: Environment,
    val isDebug: Boolean
)
