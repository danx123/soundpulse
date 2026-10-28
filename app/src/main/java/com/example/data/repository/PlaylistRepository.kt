package com.example.data.repository

import com.example.data.local.dao.PlaylistDao
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistTrackEntity
import com.example.data.local.entity.PlaylistType
import com.example.data.local.entity.PlaylistWithTracks
import com.example.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    val allPlaylistsWithTracks: Flow<List<PlaylistWithTracks>> =
        playlistDao.getPlaylistsWithTracks()

    fun getPlaylist(playlistId: Long): Flow<PlaylistWithTracks?> =
        playlistDao.getPlaylistWithTracks(playlistId)

    suspend fun createPlaylist(
        title: String,
        description: String = "",
        type: PlaylistType = PlaylistType.LOCAL
    ): Long = withContext(Dispatchers.IO) {
        val entity = PlaylistEntity(
            title = title.trim(),
            description = description.trim(),
            type = type,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(entity)
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: AudioTrack): Boolean =
        withContext(Dispatchers.IO) {
            val exists = playlistDao.isTrackInPlaylist(playlistId, track.id)
            if (!exists) {
                val entity = PlaylistTrackEntity.fromAudioTrack(track, playlistId)
                playlistDao.insertTrack(entity)
                playlistDao.updatePlaylistTimestamp(playlistId)
                true
            } else {
                false
            }
        }

    suspend fun addTracksToPlaylist(playlistId: Long, tracks: List<AudioTrack>): Int =
        withContext(Dispatchers.IO) {
            var added = 0
            for (track in tracks) {
                if (!playlistDao.isTrackInPlaylist(playlistId, track.id)) {
                    playlistDao.insertTrack(PlaylistTrackEntity.fromAudioTrack(track, playlistId, added))
                    added++
                }
            }
            if (added > 0) {
                playlistDao.updatePlaylistTimestamp(playlistId)
            }
            added
        }

    suspend fun removeTrack(playlistTrackId: Long, playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeTrack(playlistTrackId)
        playlistDao.updatePlaylistTimestamp(playlistId)
    }

    suspend fun removeTrackByTrackId(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackByTrackId(playlistId, trackId)
        playlistDao.updatePlaylistTimestamp(playlistId)
    }

    suspend fun isTrackInPlaylist(playlistId: Long, trackId: String): Boolean =
        withContext(Dispatchers.IO) {
            playlistDao.isTrackInPlaylist(playlistId, trackId)
        }
}
