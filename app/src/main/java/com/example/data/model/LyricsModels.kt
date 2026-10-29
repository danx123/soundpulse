package com.example.data.model

data class LyricLine(val timeMs: Long, val text: String)

data class LyricsData(
    val syncedLines: List<LyricLine>,
    val plainText: String?,
    val instrumental: Boolean,
    val source: String
) {
    val isSynced: Boolean get() = syncedLines.isNotEmpty()
}

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data class Success(val data: LyricsData) : LyricsUiState
    data object NotFound : LyricsUiState
    data class Unavailable(val reason: String) : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}

object LrcParser {
    private val TIMESTAMP = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val WORD_TAG = Regex("<\\d+:\\d+(?:[.:]\\d+)?>")

    /** Parse teks format LRC. Tag metadata seperti [ar:...] diabaikan. */
    fun parse(lrc: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        for (raw in lrc.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val matches = TIMESTAMP.findAll(line).toList()
            if (matches.isEmpty()) continue

            val text = line.substring(matches.last().range.last + 1)
                .replace(WORD_TAG, "")
                .trim()
            for (m in matches) {
                val minutes = m.groupValues[1].toLong()
                val seconds = m.groupValues[2].toLong()
                val frac = m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                out.add(LyricLine(minutes * 60_000 + seconds * 1_000 + frac, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /** Index baris aktif: baris terakhir dengan timeMs <= positionMs, atau -1. */
    fun activeIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.size - 1
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return ans
    }
}
