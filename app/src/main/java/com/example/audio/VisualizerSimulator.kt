package com.example.audio

import kotlin.math.sin
import kotlin.random.Random

class VisualizerSimulator {
    private val bandCount = 28
    private val currentAmplitudes = FloatArray(bandCount) { 0.1f }

    fun updateAmplitudes(
        isPlaying: Boolean,
        positionMs: Long,
        bassBoost: Int,
        equalizerBands: List<Int>?
    ): List<Float> {
        if (!isPlaying) {
            // Decay gradually to idle baseline
            for (i in 0 until bandCount) {
                currentAmplitudes[i] = (currentAmplitudes[i] * 0.85f).coerceAtLeast(0.04f)
            }
            return currentAmplitudes.toList()
        }

        val timeSec = positionMs / 1000f
        val bassFactor = 1f + (bassBoost / 1000f) * 0.8f

        for (i in 0 until bandCount) {
            val freqNorm = i.toFloat() / bandCount
            // Harmonic wave equations simulating rhythm and frequency spread
            val wave1 = (sin((timeSec * 6.0 + i * 0.45).toDouble()).toFloat() + 1f) * 0.5f
            val wave2 = (sin((timeSec * 14.0 + i * 0.9).toDouble()).toFloat() + 1f) * 0.35f
            val wave3 = (sin((timeSec * 2.5 - i * 0.2).toDouble()).toFloat() + 1f) * 0.2f
            val jitter = Random.nextFloat() * 0.15f

            var target = (wave1 * 0.5f + wave2 * 0.3f + wave3 * 0.2f + jitter)

            // Bass enhancement for lower third of frequencies
            if (i < 8) {
                target *= bassFactor
            }

            // Apply equalizer influence if present
            if (equalizerBands != null && equalizerBands.isNotEmpty()) {
                val eqBandIndex = (freqNorm * (equalizerBands.size - 1)).toInt().coerceIn(0, equalizerBands.size - 1)
                val eqGain = equalizerBands[eqBandIndex] / 1000f // in dB
                target *= (1f + (eqGain * 0.3f)).coerceIn(0.2f, 2.0f)
            }

            // Smooth interpolation
            currentAmplitudes[i] = currentAmplitudes[i] * 0.4f + target.coerceIn(0.05f, 1.0f) * 0.6f
        }

        return currentAmplitudes.toList()
    }
}
