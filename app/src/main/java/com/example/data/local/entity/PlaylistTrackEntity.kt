package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AudioFormat
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["playlistId", "trackId"])
    ]
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val uri: String,
    val sourceType: String = AudioSourceType.LOCAL_FILE.name,
    val format: String = AudioFormat.MP3.name,
    val bitrateKbps: Int? = 320,
    val sampleRateHz: Int? = 44100,
    val bitDepth: Int? = 16,
    val fileSizeFormatted: String = "",
    val cloudProvider: String? = null,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toAudioTrack(): AudioTrack {
        val parsedSource = try {
            AudioSourceType.valueOf(sourceType)
        } catch (e: Exception) {
            AudioSourceType.LOCAL_FILE
        }

        val parsedFormat = try {
            AudioFormat.valueOf(format)
        } catch (e: Exception) {
            AudioFormat.MP3
        }

        return AudioTrack(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = uri,
            sourceType = parsedSource,
            format = parsedFormat,
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            fileSizeFormatted = fileSizeFormatted,
            cloudProvider = cloudProvider
        )
    }

    companion object {
        fun fromAudioTrack(track: AudioTrack, playlistId: Long, orderIndex: Int = 0): PlaylistTrackEntity {
            return PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                uri = track.uri,
                sourceType = track.sourceType.name,
                format = track.format.name,
                bitrateKbps = track.bitrateKbps,
                sampleRateHz = track.sampleRateHz,
                bitDepth = track.bitDepth,
                fileSizeFormatted = track.fileSizeFormatted,
                cloudProvider = track.cloudProvider,
                orderIndex = orderIndex,
                addedAt = System.currentTimeMillis()
            )
        }
    }
}
