package com.eagleeye.data.db

import androidx.room.*
import com.eagleeye.data.LanDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface LanDeviceDao {

    @Query("SELECT * FROM lan_devices ORDER BY lastSeen DESC")
    fun observeAll(): Flow<List<LanDevice>>

    @Query("SELECT * FROM lan_devices ORDER BY lastSeen DESC")
    suspend fun getAll(): List<LanDevice>

    @Query("SELECT * FROM lan_devices WHERE mac = :mac LIMIT 1")
    suspend fun getByMac(mac: String): LanDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: LanDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<LanDevice>)

    @Query("UPDATE lan_devices SET isKnown = :known WHERE mac = :mac")
    suspend fun setKnown(mac: String, known: Boolean)

    @Query("UPDATE lan_devices SET alias = :alias WHERE mac = :mac")
    suspend fun setAlias(mac: String, alias: String)

    @Query("DELETE FROM lan_devices")
    suspend fun deleteAll()
}
