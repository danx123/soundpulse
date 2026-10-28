package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlaylistType(val displayName: String) {
    LOCAL("Lokal"),
    CLOUD("Cloud Storage"),
    HYBRID("Campuran (Lokal & Cloud)")
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: PlaylistType = PlaylistType.LOCAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val coverArtUrl: String? = null
)
