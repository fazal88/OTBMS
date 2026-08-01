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

class MainActivity : ComponentActivity(), ActivityHolder.FilePickerProvider {
    companion object {
        var instance: MainActivity? = null
    }

    private var filePickerCallback: ((ByteArray?) -> Unit)? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        handlePickerResult(uri)
    }

    private val docPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        handlePickerResult(uri)
    }

    private fun handlePickerResult(uri: android.net.Uri?) {
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                filePickerCallback?.invoke(bytes)
            } catch (e: Exception) {
                filePickerCallback?.invoke(null)
            }
        } else {
            filePickerCallback?.invoke(null)
        }
        filePickerCallback = null
    }

    override fun pickImage(callback: (ByteArray?) -> Unit) {
        this.filePickerCallback = callback
        imagePickerLauncher.launch("image/*")
    }

    override fun pickImageOrPdf(callback: (ByteArray?) -> Unit) {
        this.filePickerCallback = callback
        docPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
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
        ActivityHolder.init(this)
        ActivityHolder.setFilePickerProvider(this)

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
        ActivityHolder.setFilePickerProvider(null)
        if (instance == this) instance = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}