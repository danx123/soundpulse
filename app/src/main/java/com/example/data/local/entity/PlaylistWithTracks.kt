package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PlaylistWithTracks(
    @Embedded
    val playlist: PlaylistEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val tracks: List<PlaylistTrackEntity>
) {
    val trackCount: Int
        get() = tracks.size

    val totalDurationMs: Long
        get() = tracks.sumOf { it.durationMs }

    val hasCloudTracks: Boolean
        get() = tracks.any { it.sourceType == "CLOUD_STORAGE" || it.sourceType == "STREAMING_RADIO" }

    val hasLocalTracks: Boolean
        get() = tracks.any { it.sourceType == "LOCAL_FILE" || it.sourceType == "LOCAL_FOLDER" || it.sourceType == "BUILTIN_DEMO" }
}
