package com.eagleeye.data.db

import androidx.room.*
import com.eagleeye.data.MacProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface MacProfileDao {
    @Query("SELECT * FROM mac_profiles ORDER BY ssid ASC")
    fun observeAll(): Flow<List<MacProfile>>

    @Query("SELECT * FROM mac_profiles WHERE ssid = :ssid LIMIT 1")
    suspend fun getForSsid(ssid: String): MacProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: MacProfile)

    @Delete
    suspend fun delete(profile: MacProfile)

    @Query("UPDATE mac_profiles SET lastRotated = :timestamp WHERE ssid = :ssid")
    suspend fun updateLastRotated(ssid: String, timestamp: Long)
}
