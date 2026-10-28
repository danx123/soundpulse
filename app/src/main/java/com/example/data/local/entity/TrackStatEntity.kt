package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_stats")
data class TrackStatEntity(
    @PrimaryKey
    val trackId: String,
    val title: String,
    val artist: String,
    val playCount: Int = 1,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val totalPlayTimeMs: Long = 0L
)
