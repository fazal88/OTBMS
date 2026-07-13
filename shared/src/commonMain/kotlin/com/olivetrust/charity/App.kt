package com.olivetrust.charity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.Navigator
import com.olivetrust.charity.di.appModule
import com.olivetrust.charity.ui.screens.SplashScreen
import com.olivetrust.charity.ui.theme.OliveTheme
import androidx.compose.ui.tooling.preview.Preview
import com.olivetrust.charity.data.util.DatabaseSeeder
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import org.koin.compose.koinInject
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.model.UserRole
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
@Preview
fun App(config: AppConfig? = null) {
    // Uncomment the line below to seed the database once, then comment it back out.
//     LaunchedEffect(Unit) { DatabaseSeeder.seed() }

    KoinApplication(application = {
        if (config != null) {
            modules(module { single { config } })
        }
        modules(appModule)
    }) {
        val authRepository: AuthRepository = koinInject()
        val user by authRepository.currentUser.collectAsState(null)
        
        LaunchedEffect(user) {
            val role = user?.role
            val isSensitiveRole = role == UserRole.EMPLOYEE || role == UserRole.COLLECTOR
            setScreenshotProtection(isSensitiveRole)
        }

        OliveTheme {
            Navigator(SplashScreen())
        }
    }
}

