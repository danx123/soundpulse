package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TrackStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackStatDao {

    @Query("SELECT * FROM track_stats ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 50): Flow<List<TrackStatEntity>>

    @Query("SELECT * FROM track_stats ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<TrackStatEntity>>

    @Query("SELECT * FROM track_stats WHERE trackId = :trackId")
    suspend fun getStat(trackId: String): TrackStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stat: TrackStatEntity)

    @Query("UPDATE track_stats SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE trackId = :trackId")
    suspend fun incrementPlayCount(trackId: String, timestamp: Long = System.currentTimeMillis()): Int
}
