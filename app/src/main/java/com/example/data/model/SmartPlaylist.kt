package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class SmartPlaylistType {
    MOST_PLAYED,
    RECENTLY_PLAYED,
    MOOD_CHILL,
    MOOD_ENERGY,
    LOSSLESS_VAULT,
    AI_CUSTOM
}

data class SmartPlaylist(
    val id: String,
    val title: String,
    val description: String,
    val badge: String,
    val type: SmartPlaylistType,
    val tracks: List<AudioTrack>,
    val accentColor: Color = Color(0xFF00F0FF)
) {
    val trackCount: Int
        get() = tracks.size

    val totalDurationMs: Long
        get() = tracks.sumOf { it.durationMs }
}

data class AiMusicRecommendation(
    val id: String,
    val genreOrMood: String,
    val title: String,
    val explanation: String,
    val iconName: String,
    val sampleTrack: AudioTrack?,
    val matchingTrackCount: Int
)
