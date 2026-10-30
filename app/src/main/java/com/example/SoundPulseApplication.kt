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
        // Jika belum dikonfigurasi, app tetap berjalan penuh secara offline/lokal tanpa crash.
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                Firebase.appCheck.installAppCheckProviderFactory(
                    if (BuildConfig.DEBUG) {
                        DebugAppCheckProviderFactory.getInstance()
                    } else {
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    }
                )
            }
        } catch (e: Throwable) {
            Log.w("SoundPulseApp", "Firebase belum dikonfigurasi atau tidak aktif: ${e.message}")
        }
    }
}
