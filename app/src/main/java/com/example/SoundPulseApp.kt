package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class SoundPulseApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Tanpa google-services.json Firebase tidak aktif: AI DJ & login Google dinonaktifkan,
        // sisa app tetap berjalan normal.
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        if (FirebaseApp.getApps(this).isEmpty()) return

        // Firebase AI Logic mewajibkan App Check (otomatis ditegakkan mulai 2 Nov 2026).
        try {
            Firebase.appCheck.installAppCheckProviderFactory(
                if (BuildConfig.DEBUG) {
                    // Build debug: token debug muncul di Logcat dan harus didaftarkan di Firebase Console
                    DebugAppCheckProviderFactory.getInstance()
                } else {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }
            )
        } catch (e: Exception) {
            Log.w("SoundPulseApp", "Gagal memasang App Check", e)
        }
    }
}
