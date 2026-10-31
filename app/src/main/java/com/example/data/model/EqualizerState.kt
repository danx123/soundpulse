package com.example.data.model

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMillibels: Int
) {
    val frequencyLabel: String
        get() = when {
            centerFreqHz >= 1000 -> "${centerFreqHz / 1000}k"
            else -> "${centerFreqHz}Hz"
        }

    val decibelValue: Float
        get() = levelMillibels / 100f
}

data class EqualizerPreset(
    val name: String,
    val levelsMillibels: List<Int>,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val reverb: String = "None"
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<EqualizerBand> = defaultBands(),
    val minLevelMillibels: Int = -1500,
    val maxLevelMillibels: Int = 1500,
    val currentPreset: String = "Flat",
    val bassBoostStrength: Int = 350,       // 0 to 1000
    val virtualizerStrength: Int = 400,     // 0 to 1000
    val reverbEffect: String = "None",
    val isReplayGainEnabled: Boolean = true,
    val replayGainTargetDb: Float = 6.0f
) {
    companion object {
        fun defaultBands(): List<EqualizerBand> = listOf(
            EqualizerBand(0, 60, 0),
            EqualizerBand(1, 230, 0),
            EqualizerBand(2, 910, 0),
            EqualizerBand(3, 3600, 0),
            EqualizerBand(4, 14000, 0)
        )

        val PRESETS = listOf(
            EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0), bassBoost = 0, virtualizer = 0, reverb = "None"),
            EqualizerPreset("Bass Boost", listOf(900, 600, 200, 0, -200), bassBoost = 850, virtualizer = 300, reverb = "Small Room"),
            EqualizerPreset("Rock", listOf(500, 300, -200, 400, 600), bassBoost = 500, virtualizer = 500, reverb = "Medium Room"),
            EqualizerPreset("Pop", listOf(-200, 200, 500, 300, -100), bassBoost = 400, virtualizer = 400, reverb = "Small Room"),
            EqualizerPreset("Jazz", listOf(300, 100, -100, 200, 400), bassBoost = 300, virtualizer = 600, reverb = "Large Room"),
            EqualizerPreset("Electronic", listOf(800, 500, -100, 400, 700), bassBoost = 800, virtualizer = 700, reverb = "Hall"),
            EqualizerPreset("Classical", listOf(400, 200, -200, 300, 400), bassBoost = 200, virtualizer = 750, reverb = "Large Hall"),
            EqualizerPreset("Hip Hop", listOf(800, 400, 100, 200, 300), bassBoost = 900, virtualizer = 400, reverb = "Small Room"),
            EqualizerPreset("Acoustic", listOf(300, 200, 100, 300, 400), bassBoost = 200, virtualizer = 350, reverb = "Plate"),
            EqualizerPreset("Vocal Booster", listOf(-400, 200, 700, 500, -200), bassBoost = 100, virtualizer = 200, reverb = "Small Room")
        )
    }
}
