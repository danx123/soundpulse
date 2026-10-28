package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.AudioFormat
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.data.model.CloudProviderPreset
import com.example.data.model.StreamingStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "SoundPulseRepo"

class AudioRepository(private val context: Context) {

    private val _tracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()

    private val _cloudTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val cloudTracks: StateFlow<List<AudioTrack>> = _cloudTracks.asStateFlow()

    private val _streamingStations = MutableStateFlow<List<StreamingStation>>(emptyList())
    val streamingStations: StateFlow<List<StreamingStation>> = _streamingStations.asStateFlow()

    private val _cloudPresets = MutableStateFlow<List<CloudProviderPreset>>(emptyList())
    val cloudPresets: StateFlow<List<CloudProviderPreset>> = _cloudPresets.asStateFlow()

    init {
        loadDefaultStations()
        loadDefaultCloudPresets()
        loadDefaultTracks()
    }

    private fun loadDefaultTracks() {
        val initialTracks = listOf(
            AudioTrack(
                id = "demo_1",
                title = "Cybernetic Horizon (Lossless Synth)",
                artist = "Pulse Studio",
                album = "Neon Drift 2088",
                durationMs = 214000L,
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                sourceType = AudioSourceType.BUILTIN_DEMO,
                format = AudioFormat.MP3,
                bitrateKbps = 320,
                sampleRateHz = 44100,
                fileSizeFormatted = "8.2 MB",
                artworkUrl = null
            ),
            AudioTrack(
                id = "demo_2",
                title = "Lo-Fi Midnight Coffee (Chillhop)",
                artist = "Aura Wave",
                album = "Rainy Rooftops",
                durationMs = 186000L,
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                sourceType = AudioSourceType.BUILTIN_DEMO,
                format = AudioFormat.FLAC,
                bitrateKbps = 1411,
                sampleRateHz = 96000,
                bitDepth = 24,
                fileSizeFormatted = "32.4 MB (Hi-Res)",
                artworkUrl = null
            ),
            AudioTrack(
                id = "demo_3",
                title = "Deep Space Pulse (Sub-Bass Test)",
                artist = "Frequency Lab",
                album = "Acoustic Spectrum",
                durationMs = 245000L,
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                sourceType = AudioSourceType.BUILTIN_DEMO,
                format = AudioFormat.WAV,
                bitrateKbps = 1411,
                sampleRateHz = 48000,
                bitDepth = 24,
                fileSizeFormatted = "41.0 MB",
                artworkUrl = null
            ),
            AudioTrack(
                id = "demo_4",
                title = "Solar Flare Groove (Funk Fusion)",
                artist = "The Quantum Band",
                album = "Starlight Sessions",
                durationMs = 298000L,
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                sourceType = AudioSourceType.BUILTIN_DEMO,
                format = AudioFormat.AAC,
                bitrateKbps = 256,
                sampleRateHz = 44100,
                fileSizeFormatted = "6.4 MB",
                artworkUrl = null
            ),
            AudioTrack(
                id = "demo_5",
                title = "Ethereal Ambient Reverie",
                artist = "Echoes of Silence",
                album = "Infinite Horizons",
                durationMs = 312000L,
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
                sourceType = AudioSourceType.BUILTIN_DEMO,
                format = AudioFormat.OGG,
                bitrateKbps = 320,
                sampleRateHz = 48000,
                fileSizeFormatted = "7.8 MB",
                artworkUrl = null
            )
        )
        _tracks.value = initialTracks
    }

    private fun loadDefaultStations() {
        _streamingStations.value = listOf(
            StreamingStation(
                id = "station_1",
                name = "SomaFM Groove Salad",
                genre = "Ambient / Chillout",
                streamUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
                description = "Downtempo electronic and ambient grooves for relaxation and focus.",
                bitrateBadge = "128k MP3",
                country = "San Francisco, USA",
                listenerCount = "12.8k"
            ),
            StreamingStation(
                id = "station_2",
                name = "SomaFM Secret Agent",
                genre = "Spy / Lounge / Surf",
                streamUrl = "https://ice1.somafm.com/secretagent-128-mp3",
                description = "The soundtrack for your stylish secret mission with 60s lounge and spy film gems.",
                bitrateBadge = "128k MP3",
                country = "USA",
                listenerCount = "5.4k"
            ),
            StreamingStation(
                id = "station_3",
                name = "SomaFM Drone Zone",
                genre = "Deep Space Drone",
                streamUrl = "https://ice1.somafm.com/dronezone-128-mp3",
                description = "Atmospheric textures with minimal beats, ideal for sleep, meditation, and deep thought.",
                bitrateBadge = "128k MP3",
                country = "Global",
                listenerCount = "8.1k"
            ),
            StreamingStation(
                id = "station_4",
                name = "SomaFM Indie Pop Rocks",
                genre = "Indie Rock & Pop",
                streamUrl = "https://ice1.somafm.com/indiepop-128-mp3",
                description = "New, independent, and undiscovered guitar pop and modern underground rock.",
                bitrateBadge = "128k MP3",
                country = "USA",
                listenerCount = "6.9k"
            ),
            StreamingStation(
                id = "station_5",
                name = "SomaFM DEF CON Radio",
                genre = "Hacker Electronic",
                streamUrl = "https://ice1.somafm.com/defcon-128-mp3",
                description = "Music for hacking, coding, and nocturnal computing sessions.",
                bitrateBadge = "128k MP3",
                country = "Las Vegas, USA",
                listenerCount = "9.7k"
            ),
            StreamingStation(
                id = "station_6",
                name = "SomaFM Suburbs of Goa",
                genre = "Psychedelic World Chill",
                streamUrl = "https://ice1.somafm.com/suburbsofgoa-128-mp3",
                description = "Desi-influenced Asian world beats, sitars, and hypnotic electronic chill.",
                bitrateBadge = "128k MP3",
                country = "Goa / Global",
                listenerCount = "3.2k"
            )
        )
    }

    private fun loadDefaultCloudPresets() {
        val presets = listOf(
            CloudProviderPreset(
                id = "gcs",
                name = "Google Cloud Storage",
                providerType = "Object Storage",
                description = "Stream audio directly from public or signed GCS buckets.",
                defaultBaseUrl = "https://storage.googleapis.com/",
                iconName = "cloud",
                demoAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                sampleTrackTitle = "GCS Master Stream (Flac 96kHz)",
                sampleFormat = AudioFormat.FLAC
            ),
            CloudProviderPreset(
                id = "archive",
                name = "Internet Archive (FLAC)",
                providerType = "Open Digital Library",
                description = "Millions of live concert recordings and lossless archival audio.",
                defaultBaseUrl = "https://archive.org/download/",
                iconName = "library",
                demoAudioUrl = "https://ia800508.us.archive.org/15/items/testmp3testfile/mpthreetest.mp3",
                sampleTrackTitle = "Open Archive Live Session",
                sampleFormat = AudioFormat.MP3
            ),
            CloudProviderPreset(
                id = "nextcloud",
                name = "Nextcloud / WebDAV",
                providerType = "Private Cloud",
                description = "Personal cloud media server with direct HTTP audio streaming.",
                defaultBaseUrl = "https://cloud.example.com/remote.php/webdav/",
                iconName = "server",
                demoAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                sampleTrackTitle = "Private Cloud High-Res Audio",
                sampleFormat = AudioFormat.WAV
            ),
            CloudProviderPreset(
                id = "custom_url",
                name = "Custom Direct Audio URL",
                providerType = "HTTP/HTTPS Stream",
                description = "Play any direct link (.mp3, .flac, .wav, .aac, .m4a, .ogg) anywhere.",
                defaultBaseUrl = "https://",
                iconName = "link",
                demoAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                sampleTrackTitle = "Cloud Web Audio Stream",
                sampleFormat = AudioFormat.MP3
            )
        )
        _cloudPresets.value = presets

        // Populate initial cloud tracks
        _cloudTracks.value = listOf(
            AudioTrack(
                id = "cloud_sample_1",
                title = "GCS High-Res Symphony No. 9",
                artist = "Beethoven Philharmonia",
                album = "Cloud Storage Vault",
                durationMs = 380000L,
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                sourceType = AudioSourceType.CLOUD_STORAGE,
                format = AudioFormat.FLAC,
                bitrateKbps = 1411,
                sampleRateHz = 96000,
                bitDepth = 24,
                cloudProvider = "Google Cloud Storage",
                fileSizeFormatted = "54 MB"
            ),
            AudioTrack(
                id = "cloud_sample_2",
                title = "Internet Archive Live Acoustic",
                artist = "Archive Open Session",
                album = "Live Concert Audio Archive",
                durationMs = 270000L,
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                sourceType = AudioSourceType.CLOUD_STORAGE,
                format = AudioFormat.MP3,
                bitrateKbps = 320,
                sampleRateHz = 44100,
                cloudProvider = "Internet Archive",
                fileSizeFormatted = "9.8 MB"
            ),
            AudioTrack(
                id = "cloud_sample_3",
                title = "Nextcloud Master Tape",
                artist = "Studio Synth",
                album = "Private Cloud Storage",
                durationMs = 310000L,
                uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                sourceType = AudioSourceType.CLOUD_STORAGE,
                format = AudioFormat.WAV,
                bitrateKbps = 1411,
                sampleRateHz = 48000,
                bitDepth = 24,
                cloudProvider = "Nextcloud WebDAV",
                fileSizeFormatted = "48 MB"
            )
        )
    }

    suspend fun importLocalUrisAndReturn(uris: List<Uri>): List<AudioTrack> = withContext(Dispatchers.IO) {
        val newTracks = mutableListOf<AudioTrack>()
        val mmr = MediaMetadataRetriever()

        for (uri in uris) {
            try {
                mmr.setDataSource(context, uri)
                val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                    ?: "Audio Track"
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Local Files"
                val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                val bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let { it / 1000 } ?: 320
                val mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

                val ext = uri.lastPathSegment?.substringAfterLast('.', "") ?: "mp3"
                val format = when {
                    mimeType?.contains("flac") == true -> AudioFormat.FLAC
                    mimeType?.contains("wav") == true -> AudioFormat.WAV
                    mimeType?.contains("aac") == true -> AudioFormat.AAC
                    mimeType?.contains("ogg") == true -> AudioFormat.OGG
                    mimeType?.contains("opus") == true -> AudioFormat.OPUS
                    else -> AudioFormat.fromExtension(ext)
                }

                newTracks.add(
                    AudioTrack(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uri = uri.toString(),
                        sourceType = AudioSourceType.LOCAL_FILE,
                        format = format,
                        bitrateKbps = bitrate,
                        sampleRateHz = if (format.isLossless) 96000 else 44100,
                        fileSizeFormatted = "Local Audio",
                        artworkUrl = null
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error extracting metadata for $uri", e)
                val fallbackTitle = uri.lastPathSegment?.substringAfterLast('/') ?: "Local Audio File"
                newTracks.add(
                    AudioTrack(
                        id = UUID.randomUUID().toString(),
                        title = fallbackTitle,
                        artist = "Local Artist",
                        album = "Imported",
                        durationMs = 0L,
                        uri = uri.toString(),
                        sourceType = AudioSourceType.LOCAL_FILE,
                        format = AudioFormat.MP3,
                        bitrateKbps = 320,
                        sampleRateHz = 44100,
                        fileSizeFormatted = "Local File"
                    )
                )
            }
        }

        try {
            mmr.release()
        } catch (e: Exception) {
            // ignore
        }

        if (newTracks.isNotEmpty()) {
            _tracks.value = newTracks + _tracks.value
        }

        newTracks
    }

    suspend fun importLocalUris(uris: List<Uri>): Int {
        return importLocalUrisAndReturn(uris).size
    }

    fun extractFolderName(treeUri: Uri): String {
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val clean = docId.substringAfterLast(':').substringAfterLast('/')
            if (clean.isNotBlank()) return clean
        } catch (e: Exception) {
            // ignore
        }
        return treeUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "Folder Musik"
    }

    suspend fun loadFolderTracks(treeUri: Uri): Pair<String, List<AudioTrack>> = withContext(Dispatchers.IO) {
        val folderName = extractFolderName(treeUri)
        val discoveredUris = mutableListOf<Uri>()
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx)
                    val mime = cursor.getString(mimeIdx)

                    val isAudio = mime?.startsWith("audio/") == true ||
                            name.endsWith(".mp3", ignoreCase = true) ||
                            name.endsWith(".flac", ignoreCase = true) ||
                            name.endsWith(".wav", ignoreCase = true) ||
                            name.endsWith(".m4a", ignoreCase = true) ||
                            name.endsWith(".aac", ignoreCase = true) ||
                            name.endsWith(".ogg", ignoreCase = true) ||
                            name.endsWith(".opus", ignoreCase = true)

                    if (isAudio) {
                        val fileDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        discoveredUris.add(fileDocUri)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder tree $treeUri", e)
        }

        val loadedTracks = if (discoveredUris.isNotEmpty()) {
            importLocalUrisAndReturn(discoveredUris)
        } else {
            emptyList()
        }

        Pair(folderName, loadedTracks)
    }

    suspend fun scanFolderTree(treeUri: Uri): Int {
        return loadFolderTracks(treeUri).second.size
    }

    /**
     * Scans audio tracks from MediaStore. Supported across Android 9+ (API 28+).
     * Works with READ_EXTERNAL_STORAGE on API 28-32, and READ_MEDIA_AUDIO on API 33+.
     */
    suspend fun scanMediaStoreAudio(): Int = withContext(Dispatchers.IO) {
        val discoveredTracks = mutableListOf<AudioTrack>()
        try {
            val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                contentUri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Device Audio"
                    val duration = cursor.getLong(durationCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol)

                    val trackUri = ContentUris.withAppendedId(contentUri, id)

                    val format = when {
                        mime?.contains("flac") == true -> AudioFormat.FLAC
                        mime?.contains("wav") == true -> AudioFormat.WAV
                        mime?.contains("aac") == true -> AudioFormat.AAC
                        mime?.contains("ogg") == true -> AudioFormat.OGG
                        mime?.contains("opus") == true -> AudioFormat.OPUS
                        mime?.contains("mp4") == true || mime?.contains("m4a") == true -> AudioFormat.AAC
                        else -> AudioFormat.MP3
                    }

                    val sizeFormatted = if (sizeBytes > 0) {
                        String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
                    } else {
                        "Local File"
                    }

                    discoveredTracks.add(
                        AudioTrack(
                            id = "mediastore_$id",
                            title = title,
                            artist = if (artist.equals("<unknown>", ignoreCase = true)) "Unknown Artist" else artist,
                            album = if (album.equals("<unknown>", ignoreCase = true)) "Device Audio" else album,
                            durationMs = duration,
                            uri = trackUri.toString(),
                            sourceType = AudioSourceType.LOCAL_FILE,
                            format = format,
                            bitrateKbps = if (format.isLossless) 1411 else 320,
                            sampleRateHz = if (format.isLossless) 96000 else 44100,
                            fileSizeFormatted = sizeFormatted
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MediaStore audio", e)
        }

        if (discoveredTracks.isNotEmpty()) {
            val existingUris = _tracks.value.map { it.uri }.toSet()
            val newTracks = discoveredTracks.filter { it.uri !in existingUris }
            if (newTracks.isNotEmpty()) {
                _tracks.value = newTracks + _tracks.value
            }
            return@withContext newTracks.size
        }
        return@withContext 0
    }

    fun addCloudTrack(
        title: String,
        artist: String,
        url: String,
        provider: String,
        format: AudioFormat = AudioFormat.MP3
    ): AudioTrack {
        val newTrack = AudioTrack(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Cloud Audio Track" },
            artist = artist.ifBlank { "Cloud Stream" },
            album = provider,
            durationMs = 0L,
            uri = url,
            sourceType = AudioSourceType.CLOUD_STORAGE,
            format = format,
            bitrateKbps = if (format.isLossless) 1411 else 320,
            sampleRateHz = if (format.isLossless) 96000 else 44100,
            cloudProvider = provider,
            fileSizeFormatted = "Cloud Stream"
        )
        _cloudTracks.value = listOf(newTrack) + _cloudTracks.value
        return newTrack
    }

    fun addCustomStation(name: String, genre: String, url: String, desc: String) {
        val station = StreamingStation(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Custom Web Radio" },
            genre = genre.ifBlank { "Internet Radio" },
            streamUrl = url,
            description = desc.ifBlank { "Custom user streaming channel" },
            bitrateBadge = "Custom Stream",
            country = "Online",
            isLive = true
        )
        _streamingStations.value = listOf(station) + _streamingStations.value
    }

    fun toggleFavorite(trackId: String) {
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _cloudTracks.value = _cloudTracks.value.map {
            if (it.id == trackId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }
}
