package com.eagleeye.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MacType {
    REAL,           // Actual hardware MAC
    RANDOMIZED,     // Android per-network randomization
    CUSTOM,         // User-set via root
    UNKNOWN
}

data class MacInfo(
    val currentMac: String,
    val type: MacType,
    val vendor: String,
    val isRandomized: Boolean,
    val hardwareMac: String   // best-effort, may be empty without root
)

@Entity(tableName = "mac_profiles")
data class MacProfile(
    @PrimaryKey val ssid: String,
    val mac: String,            // MAC to use for this SSID
    val isAutoRotate: Boolean = false,
    val rotateIntervalHours: Int = 24,
    val lastRotated: Long = 0L,
    val notes: String = ""
)
