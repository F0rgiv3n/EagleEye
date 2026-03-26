package com.eagleeye.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eagleeye.data.LanDevice
import com.eagleeye.data.MacProfile
import com.eagleeye.data.NetworkEvent

@Database(entities = [LanDevice::class, MacProfile::class, NetworkEvent::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lanDeviceDao(): LanDeviceDao
    abstract fun macProfileDao(): MacProfileDao
    abstract fun networkEventDao(): NetworkEventDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eagleeye.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
