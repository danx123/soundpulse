package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class SoundPulseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Firebase secara aman jika tersedia konfigurasi (google-services.json).
        // App Check dikonfigurasi di MainActivity.onCreate() bersama intent.
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            Log.w("SoundPulseApp", "Firebase belum aktif: ${e.message}")
        }
    }
}
