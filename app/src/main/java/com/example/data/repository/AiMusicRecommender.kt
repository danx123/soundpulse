package com.example.data.repository

import androidx.compose.ui.graphics.Color
import com.example.data.local.entity.TrackStatEntity
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.AiMusicRecommendation
import com.example.data.model.SmartPlaylist
import com.example.data.model.SmartPlaylistType
import java.util.UUID

class AiMusicRecommender {

    fun generateSmartPlaylists(
        allTracks: List<AudioTrack>,
        stats: List<TrackStatEntity>
    ): List<SmartPlaylist> {
        val playlists = mutableListOf<SmartPlaylist>()
        val trackMap = allTracks.associateBy { it.id }

        // 1. Most Played Smart Playlist (Yang Sering Banyak Diputar)
        val mostPlayedTracks = stats.sortedByDescending { it.playCount }
            .mapNotNull { stat -> trackMap[stat.trackId] }
            .distinctBy { it.id }

        if (mostPlayedTracks.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "smart_most_played",
                    title = "Paling Sering Diputar",
                    description = "Daftar lagu favorit yang paling banyak didengarkan berdasarkan kebiasaan Anda.",
                    badge = "MOST PLAYED",
                    type = SmartPlaylistType.MOST_PLAYED,
                    tracks = mostPlayedTracks,
                    accentColor = Color(0xFFFF9E00) // Golden Amber
                )
            )
        }

        // 2. AI Mix: Lo-Fi & Chill Focus
        val chillTracks = allTracks.filter { track ->
            val text = (track.title + " " + track.artist + " " + track.album).lowercase()
            text.contains("lo-fi") || text.contains("chill") || text.contains("ambient") ||
                    text.contains("reverie") || text.contains("coffee") || text.contains("quiet")
        }
        if (chillTracks.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "smart_chill_focus",
                    title = "AI Chillhop & Focus Flow",
                    description = "Kurasi musik santai tanpa vokal bising untuk konsentrasi belajar dan bekerja.",
                    badge = "AI CURATED",
                    type = SmartPlaylistType.MOOD_CHILL,
                    tracks = chillTracks,
                    accentColor = Color(0xFF00F0FF) // Electric Cyan
                )
            )
        }

        // 3. AI Mix: Hi-Res Audiophile Studio Masters (FLAC & WAV)
        val losslessTracks = allTracks.filter { it.format.isLossless }
        if (losslessTracks.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "smart_lossless_masters",
                    title = "Audiophile Hi-Res Vault",
                    description = "Track dengan format Lossless 24-bit / 96kHz (FLAC & WAV) berkejernihan maksimal.",
                    badge = "LOSSLESS HI-RES",
                    type = SmartPlaylistType.LOSSLESS_VAULT,
                    tracks = losslessTracks,
                    accentColor = Color(0xFF10B981) // Studio Emerald
                )
            )
        }

        // 4. AI Mix: Cyberpunk & Synthwave Drive
        val synthTracks = allTracks.filter { track ->
            val text = (track.title + " " + track.artist + " " + track.album).lowercase()
            text.contains("synth") || text.contains("cyber") || text.contains("groove") ||
                    text.contains("pulse") || text.contains("solar") || text.contains("horizon")
        }
        if (synthTracks.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    id = "smart_cyber_energy",
                    title = "AI Cyberpunk & Night Drive",
                    description = "Irama ritmis synthwave dengan ketukan energik untuk berkendara malam.",
                    badge = "AI ENERGY",
                    type = SmartPlaylistType.MOOD_ENERGY,
                    tracks = synthTracks,
                    accentColor = Color(0xFFE024C3) // Synthwave Magenta
                )
            )
        }

        return playlists
    }

    fun generateRecommendations(
        allTracks: List<AudioTrack>,
        stats: List<TrackStatEntity>
    ): List<AiMusicRecommendation> {
        val recs = mutableListOf<AiMusicRecommendation>()

        val topStat = stats.maxByOrNull { it.playCount }
        val topTrack = allTracks.find { it.id == topStat?.trackId }

        recs.add(
            AiMusicRecommendation(
                id = "rec_habit",
                genreOrMood = "Berdasarkan Kebiasaan Mendengar",
                title = if (topTrack != null) "Lebih Banyak Mirip \"${topTrack.title}\"" else "Mulai Membangun Profil Musik Anda",
                explanation = "AI menganalisis frekuensi pemutaran Anda (${topStat?.playCount ?: 0}x pemutaran). Format favorit: Lossless FLAC & Modern Synth.",
                iconName = "auto_awesome",
                sampleTrack = topTrack ?: allTracks.firstOrNull(),
                matchingTrackCount = allTracks.size.coerceAtLeast(1)
            )
        )

        recs.add(
            AiMusicRecommendation(
                id = "rec_genre_lofi",
                genreOrMood = "Spotlight Genre: Lo-Fi Chillhop",
                title = "Alur Santai Sore Hari",
                explanation = "Direkomendasikan untuk menurunkan stres dan menaikkan gelombang alfa otak selama bekerja.",
                iconName = "headphones",
                sampleTrack = allTracks.find { it.title.contains("Lo-Fi", ignoreCase = true) } ?: allTracks.firstOrNull(),
                matchingTrackCount = allTracks.count { it.title.contains("Lo-Fi", ignoreCase = true) }
            )
        )

        recs.add(
            AiMusicRecommendation(
                id = "rec_hires",
                genreOrMood = "Optimasi Audio: High-Resolution",
                title = "Uji Dinamika Frekuensi Lossless",
                explanation = "Coba kombinasikan format FLAC/WAV dengan efek Bass Boost dan 3D Virtualizer pada Equalizer.",
                iconName = "equalizer",
                sampleTrack = allTracks.find { it.format == AudioFormat.FLAC } ?: allTracks.firstOrNull(),
                matchingTrackCount = allTracks.count { it.format.isLossless }
            )
        )

        return recs
    }

    fun createCustomAiPlaylist(prompt: String, allTracks: List<AudioTrack>): SmartPlaylist {
        val promptLower = prompt.lowercase()
        val matchingTracks = allTracks.filter { track ->
            val text = (track.title + " " + track.artist + " " + track.album).lowercase()
            when {
                promptLower.contains("santai") || promptLower.contains("relax") || promptLower.contains("tidur") ->
                    text.contains("lo-fi") || text.contains("chill") || text.contains("ambient") || text.contains("coffee")
                promptLower.contains("semangat") || promptLower.contains("energi") || promptLower.contains("olahraga") ->
                    text.contains("synth") || text.contains("groove") || text.contains("pulse") || text.contains("cyber")
                promptLower.contains("jernih") || promptLower.contains("lossless") || promptLower.contains("audiophile") ->
                    track.format.isLossless
                else -> true
            }
        }.ifEmpty { allTracks.shuffled().take(4) }

        return SmartPlaylist(
            id = "custom_ai_${UUID.randomUUID()}",
            title = "AI Mix: ${prompt.trim().replaceFirstChar { it.uppercase() }}",
            description = "Playlist cerdas otomatis dikurasi oleh AI untuk suasana: \"$prompt\"",
            badge = "AI GENERATED",
            type = SmartPlaylistType.AI_CUSTOM,
            tracks = matchingTracks,
            accentColor = Color(0xFF00F0FF)
        )
    }
}
