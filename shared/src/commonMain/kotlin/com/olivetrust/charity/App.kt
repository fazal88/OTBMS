package com.olivetrust.charity

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.Navigator
import com.olivetrust.charity.di.appModule
import com.olivetrust.charity.ui.screens.LoginScreen
import androidx.compose.ui.tooling.preview.Preview
import com.olivetrust.charity.data.util.DatabaseSeeder
import org.koin.compose.KoinApplication

@Composable
@Preview
fun App() {
    // Uncomment the line below to seed the database once, then comment it back out.
     LaunchedEffect(Unit) { DatabaseSeeder.seed() }

    KoinApplication(application = {
        modules(appModule)
    }) {
        MaterialTheme {
            Navigator(LoginScreen())
        }
    }
}
