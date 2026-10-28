package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.TrackStatDao
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistTrackEntity
import com.example.data.local.entity.PlaylistType
import com.example.data.local.entity.TrackStatEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        TrackStatEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SoundPulseDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun trackStatDao(): TrackStatDao

    companion object {
        @Volatile
        private var INSTANCE: SoundPulseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope? = null): SoundPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundPulseDatabase::class.java,
                    "soundpulse_audio.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope?
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope?.launch(Dispatchers.IO) {
                    INSTANCE?.let { database ->
                        populateInitialPlaylists(database.playlistDao())
                        populateInitialStats(database.trackStatDao())
                    }
                }
            }
        }

        suspend fun populateInitialPlaylists(dao: PlaylistDao) {
            // 1. Cloud Hi-Res Lossless Vault
            val cloudPlaylistId = dao.insertPlaylist(
                PlaylistEntity(
                    title = "Cloud Hi-Res Lossless Vault",
                    description = "Koleksi streaming kualitas tinggi dari Google Cloud Storage & Internet Archive",
                    type = PlaylistType.CLOUD
                )
            )

            dao.insertTracks(
                listOf(
                    PlaylistTrackEntity(
                        playlistId = cloudPlaylistId,
                        trackId = "cloud_sample_1",
                        title = "GCS High-Res Symphony No. 9",
                        artist = "Beethoven Philharmonia",
                        album = "Cloud Storage Vault",
                        durationMs = 380000L,
                        uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                        sourceType = "CLOUD_STORAGE",
                        format = "FLAC",
                        bitrateKbps = 1411,
                        sampleRateHz = 96000,
                        bitDepth = 24,
                        cloudProvider = "Google Cloud Storage",
                        orderIndex = 0
                    ),
                    PlaylistTrackEntity(
                        playlistId = cloudPlaylistId,
                        trackId = "cloud_sample_2",
                        title = "Internet Archive Live Acoustic",
                        artist = "Archive Open Session",
                        album = "Live Concert Audio Archive",
                        durationMs = 270000L,
                        uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                        sourceType = "CLOUD_STORAGE",
                        format = "MP3",
                        bitrateKbps = 320,
                        sampleRateHz = 44100,
                        cloudProvider = "Internet Archive",
                        orderIndex = 1
                    )
                )
            )

            // 2. Studio Master & Lo-Fi Lounge (Hybrid)
            val hybridPlaylistId = dao.insertPlaylist(
                PlaylistEntity(
                    title = "Synthwave & Lo-Fi Chill",
                    description = "Paduan musik lokal studio dan siaran santai untuk fokus dan relaksasi",
                    type = PlaylistType.HYBRID
                )
            )

            dao.insertTracks(
                listOf(
                    PlaylistTrackEntity(
                        playlistId = hybridPlaylistId,
                        trackId = "demo_1",
                        title = "Cybernetic Horizon (Lossless Synth)",
                        artist = "Pulse Studio",
                        album = "Neon Drift 2088",
                        durationMs = 214000L,
                        uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                        sourceType = "BUILTIN_DEMO",
                        format = "MP3",
                        bitrateKbps = 320,
                        sampleRateHz = 44100,
                        orderIndex = 0
                    ),
                    PlaylistTrackEntity(
                        playlistId = hybridPlaylistId,
                        trackId = "demo_2",
                        title = "Lo-Fi Midnight Coffee (Chillhop)",
                        artist = "Aura Wave",
                        album = "Rainy Rooftops",
                        durationMs = 186000L,
                        uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                        sourceType = "BUILTIN_DEMO",
                        format = "FLAC",
                        bitrateKbps = 1411,
                        sampleRateHz = 96000,
                        bitDepth = 24,
                        orderIndex = 1
                    )
                )
            )
        }

        suspend fun populateInitialStats(dao: TrackStatDao) {
            dao.insertOrUpdate(TrackStatEntity("demo_2", "Lo-Fi Midnight Coffee (Chillhop)", "Aura Wave", playCount = 14, lastPlayedAt = System.currentTimeMillis() - 3600000))
            dao.insertOrUpdate(TrackStatEntity("demo_1", "Cybernetic Horizon (Lossless Synth)", "Pulse Studio", playCount = 9, lastPlayedAt = System.currentTimeMillis() - 7200000))
            dao.insertOrUpdate(TrackStatEntity("cloud_sample_1", "GCS High-Res Symphony No. 9", "Beethoven Philharmonia", playCount = 7, lastPlayedAt = System.currentTimeMillis() - 14400000))
            dao.insertOrUpdate(TrackStatEntity("demo_3", "Deep Space Pulse (Sub-Bass Test)", "Frequency Lab", playCount = 5, lastPlayedAt = System.currentTimeMillis() - 28800000))
        }
    }
}
