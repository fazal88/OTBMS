package com.olivetrust.charity

import android.app.Application
import com.google.firebase.FirebaseApp

class OliveTrustApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ContextHolder.init(this)
        FirebaseApp.initializeApp(this)
    }
}
