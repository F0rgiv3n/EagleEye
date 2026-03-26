package com.eagleeye.data.db

import androidx.room.*
import com.eagleeye.data.NetworkEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkEventDao {

    @Query("SELECT * FROM network_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NetworkEvent>>

    @Query("SELECT * FROM network_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<NetworkEvent>>

    @Query("SELECT COUNT(*) FROM network_events WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert
    suspend fun insert(event: NetworkEvent): Long

    @Query("UPDATE network_events SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM network_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM network_events")
    suspend fun deleteAll()

    @Query("SELECT * FROM network_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): NetworkEvent?
}
