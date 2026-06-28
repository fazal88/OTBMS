package com.olivetrust.charity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.olivetrust.charity.App
import com.olivetrust.charity.AppConfig
import com.olivetrust.charity.Environment

class MainActivity : ComponentActivity() {
    companion object {
        var instance: MainActivity? = null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            println("PERMISSION_LOG: ${it.key} = ${it.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        instance = this

        requestPermissions()

        val config = AppConfig(
            environment = if (BuildConfig.FLAVOR == "uat") Environment.UAT else Environment.PRODUCTION,
            isDebug = BuildConfig.DEBUG
        )

        setContent {
            App(config)
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}