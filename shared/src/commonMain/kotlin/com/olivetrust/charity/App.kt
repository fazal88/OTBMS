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
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

@Composable
@Preview
fun App(config: AppConfig? = null) {
    setSingletonImageLoaderFactory { context ->
        println("COIL: Initializing ImageLoader with KtorNetworkFetcherFactory")
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(HttpClient {
                    install(Logging) {
                        level = LogLevel.INFO
                        logger = object : Logger {
                            override fun log(message: String) {
                                println("KTOR_LOG: $message")
                            }
                        }
                    }
                    install(io.ktor.client.plugins.DefaultRequest) {
                        header(HttpHeaders.UserAgent, "Mozilla/5.0 (Android; Mobile; rv:131.0) Gecko/131.0 Firefox/131.0")
                    }
                }))
            }
            .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
            .build()
    }

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

