package com.example.audio

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Mengubah data FFT mentah dari android.media.audiofx.Visualizer menjadi
 * [bandCount] bar bertipe log-frekuensi (40 Hz - 16 kHz) yang bereaksi pada beat.
 *
 * Format FFT Visualizer: fft[0] = Re[0], fft[1] = Re[N/2], lalu pasangan
 * (Re[k], Im[k]) di indeks 2k dan 2k+1 untuk k = 1 .. N/2-1.
 */
class FftBandProcessor(private val bandCount: Int = 28) {

    private val smoothed = FloatArray(bandCount) { MIN_LEVEL }
    private val raw = FloatArray(bandCount)

    private var peak = PEAK_FLOOR      // referensi AGC (auto gain)
    private var bassAvg = 0f           // rata-rata energi bass untuk deteksi beat
    private var kick = 0f              // pulsa beat, meluruh tiap frame

    fun reset() {
        smoothed.fill(MIN_LEVEL)
        peak = PEAK_FLOOR
        bassAvg = 0f
        kick = 0f
    }

    fun process(fft: ByteArray?, samplingRateMilliHz: Int): List<Float> {
        if (fft == null || fft.size < 16) {
            for (i in 0 until bandCount) {
                smoothed[i] = (smoothed[i] * IDLE_DECAY).coerceAtLeast(MIN_LEVEL)
            }
            kick = 0f
            return smoothed.toList()
        }

        val n = fft.size
        val bins = n / 2
        val sampleRateHz = if (samplingRateMilliHz > 0) samplingRateMilliHz / 1000f else 44100f
        val binHz = sampleRateHz / n

        val mag = FloatArray(bins)
        for (k in 1 until bins) {
            val re = fft[2 * k].toInt().toFloat()
            val im = fft[2 * k + 1].toInt().toFloat()
            mag[k] = sqrt(re * re + im * im)
        }

        // Energi per band (skala log) + tilt agar treble tidak selalu rendah
        var frameMax = 0f
        val ratio = MAX_FREQ / MIN_FREQ
        for (i in 0 until bandCount) {
            val fLo = MIN_FREQ * ratio.pow(i.toFloat() / bandCount)
            val fHi = MIN_FREQ * ratio.pow((i + 1f) / bandCount)
            val lo = fLo / binHz
            val hi = fHi / binHz

            val energy = if (hi - lo < 1f) {
                // Band lebih sempit dari 1 bin (area bass): interpolasi antar bin
                val c = (lo + hi) / 2f
                val b0 = c.toInt().coerceIn(1, bins - 2)
                val frac = (c - b0).coerceIn(0f, 1f)
                mag[b0] * (1f - frac) + mag[b0 + 1] * frac
            } else {
                val from = lo.toInt().coerceIn(1, bins - 1)
                val to = hi.toInt().coerceIn(from, bins - 1)
                var m = 0f
                for (k in from..to) if (mag[k] > m) m = mag[k]
                m
            }

            val tilt = 1f + 2.2f * (i.toFloat() / (bandCount - 1))
            raw[i] = energy * tilt
            if (raw[i] > frameMax) frameMax = raw[i]
        }

        // AGC: naik cepat, turun pelan, supaya lagu pelan/keras sama-sama hidup
        peak = maxOf(frameMax, peak * PEAK_DECAY, PEAK_FLOOR)

        // Deteksi beat dari energi bass (band 0..5)
        var bass = 0f
        val bassBands = minOf(6, bandCount)
        for (i in 0 until bassBands) bass += raw[i]
        bass /= bassBands
        val isBeat = bass > bassAvg * 1.35f + 3f && bass > peak * 0.25f
        bassAvg = bassAvg * 0.9f + bass * 0.1f
        kick = if (isBeat) 1f else kick * 0.82f

        for (i in 0 until bandCount) {
            var target = (raw[i] / peak).coerceIn(0f, 1f).pow(CONTRAST)
            if (i < 8) {
                target *= 1f + 0.35f * kick * (1f - i / 8f)
            }
            target = target.coerceIn(MIN_LEVEL, 1f)

            // Attack cepat (ikut beat), release lebih lambat (meluruh halus)
            smoothed[i] = if (target > smoothed[i]) {
                smoothed[i] + (target - smoothed[i]) * ATTACK
            } else {
                smoothed[i] + (target - smoothed[i]) * RELEASE
            }
            smoothed[i] = smoothed[i].coerceIn(MIN_LEVEL, 1f)
        }
        return smoothed.toList()
    }

    private companion object {
        const val MIN_FREQ = 40f
        const val MAX_FREQ = 16000f
        const val MIN_LEVEL = 0.04f
        const val PEAK_FLOOR = 12f
        const val PEAK_DECAY = 0.9985f
        const val CONTRAST = 1.2f
        const val ATTACK = 0.75f
        const val RELEASE = 0.22f
        const val IDLE_DECAY = 0.85f
    }
}
