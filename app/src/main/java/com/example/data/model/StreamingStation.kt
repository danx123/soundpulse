package com.example.data.model

data class StreamingStation(
    val id: String,
    val name: String,
    val genre: String,
    val streamUrl: String,
    val description: String,
    val bitrateBadge: String = "128k MP3",
    val country: String = "Global",
    val isLive: Boolean = true,
    val listenerCount: String = "4.2k",
    val artworkUrl: String? = null
)

data class CloudProviderPreset(
    val id: String,
    val name: String,
    val providerType: String, // Google Drive, Nextcloud, WebDAV, Dropbox, Internet Archive
    val description: String,
    val defaultBaseUrl: String,
    val iconName: String,
    val demoAudioUrl: String,
    val sampleTrackTitle: String,
    val sampleFormat: AudioFormat
)
