package com.eagleeye.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lan_devices")
data class LanDevice(
    @PrimaryKey val mac: String,
    val ip: String,
    val hostname: String,
    val vendor: String,
    val isOnline: Boolean,
    val latencyMs: Long,
    val openPorts: String,      // comma-separated, e.g. "22,80,443"
    val firstSeen: Long,
    val lastSeen: Long,
    val isKnown: Boolean = false,
    val alias: String = ""
) {
    val openPortList: List<Int>
        get() = if (openPorts.isBlank()) emptyList()
                else openPorts.split(",").mapNotNull { it.trim().toIntOrNull() }

    val displayName: String
        get() = when {
            alias.isNotBlank() -> alias
            hostname.isNotBlank() && hostname != ip -> hostname
            vendor.isNotBlank() -> vendor
            else -> ip
        }
}
