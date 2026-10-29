package com.example.data.local

import android.content.Context
import com.example.data.model.EqualizerState

/**
 * Menyimpan pengaturan equalizer ke SharedPreferences agar tetap ada
 * setelah app ditutup / proses dimatikan.
 */
class EqualizerStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): EqualizerState {
        val defaults = EqualizerState()
        if (!prefs.contains(KEY_ENABLED) && !prefs.contains(KEY_BANDS)) return defaults

        val savedLevels = prefs.getString(KEY_BANDS, null)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: emptyList()

        val bands = defaults.bands.mapIndexed { index, band ->
            band.copy(levelMillibels = savedLevels.getOrElse(index) { band.levelMillibels })
        }

        return defaults.copy(
            isEnabled = prefs.getBoolean(KEY_ENABLED, defaults.isEnabled),
            bands = bands,
            currentPreset = prefs.getString(KEY_PRESET, defaults.currentPreset) ?: defaults.currentPreset,
            bassBoostStrength = prefs.getInt(KEY_BASS, defaults.bassBoostStrength).coerceIn(0, 1000),
            virtualizerStrength = prefs.getInt(KEY_VIRTUALIZER, defaults.virtualizerStrength).coerceIn(0, 1000),
            reverbEffect = prefs.getString(KEY_REVERB, defaults.reverbEffect) ?: defaults.reverbEffect
        )
    }

    fun save(state: EqualizerState) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, state.isEnabled)
            .putString(KEY_BANDS, state.bands.joinToString(",") { it.levelMillibels.toString() })
            .putString(KEY_PRESET, state.currentPreset)
            .putInt(KEY_BASS, state.bassBoostStrength)
            .putInt(KEY_VIRTUALIZER, state.virtualizerStrength)
            .putString(KEY_REVERB, state.reverbEffect)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "soundpulse_equalizer"
        const val KEY_ENABLED = "enabled"
        const val KEY_BANDS = "band_levels"
        const val KEY_PRESET = "preset"
        const val KEY_BASS = "bass_boost"
        const val KEY_VIRTUALIZER = "virtualizer"
        const val KEY_REVERB = "reverb"
    }
}
