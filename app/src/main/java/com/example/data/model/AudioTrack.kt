package com.example.data.model

enum class AudioSourceType {
    LOCAL_FILE,
    LOCAL_FOLDER,
    CLOUD_STORAGE,
    STREAMING_RADIO,
    BUILTIN_DEMO
}

enum class AudioFormat(val displayName: String, val extension: String, val isLossless: Boolean) {
    MP3("MP3", "mp3", false),
    FLAC("FLAC", "flac", true),
    WAV("WAV", "wav", true),
    AAC("AAC", "aac", false),
    M4A("M4A", "m4a", false),
    OGG("OGG", "ogg", false),
    OPUS("OPUS", "opus", false),
    STREAM("LIVE STREAM", "stream", false);

    companion object {
        fun fromExtension(ext: String?): AudioFormat {
            val lower = ext?.lowercase() ?: return MP3
            return when (lower) {
                "flac" -> FLAC
                "wav" -> WAV
                "aac" -> AAC
                "m4a" -> M4A
                "ogg" -> OGG
                "opus" -> OPUS
                "mp3" -> MP3
                else -> MP3
            }
        }
    }
}

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uri: String,
    val sourceType: AudioSourceType = AudioSourceType.BUILTIN_DEMO,
    val format: AudioFormat = AudioFormat.MP3,
    val bitrateKbps: Int? = 320,
    val sampleRateHz: Int? = 44100,
    val bitDepth: Int? = 16,
    val fileSizeFormatted: String = "",
    val artworkUrl: String? = null,
    val isFavorite: Boolean = false,
    val channelCount: Int = 2,
    val cloudProvider: String? = null
)
