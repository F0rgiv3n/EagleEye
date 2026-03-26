package com.eagleeye.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType {
    NEW_DEVICE,
    DEVICE_GONE,
    ARP_SPOOF,
    EVIL_TWIN,
    DNS_CHANGED,
    WPS_DETECTED,
    OPEN_NETWORK,
    SCAN_COMPLETE,
    MONITOR_STARTED,
    MONITOR_STOPPED
}

enum class EventSeverity { CRITICAL, HIGH, MEDIUM, LOW, INFO }

@Entity(tableName = "network_events")
data class NetworkEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EventType,
    val severity: EventSeverity,
    val title: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ssid: String = "",
    val ip: String = "",
    val mac: String = "",
    val isRead: Boolean = false
)

data class MonitorConfig(
    val isEnabled: Boolean = false,
    val intervalMinutes: Int = 15,
    val notifyNewDevice: Boolean = true,
    val notifyArpSpoof: Boolean = true,
    val notifyEvilTwin: Boolean = true,
    val notifyDnsChange: Boolean = true,
    val notifyWeakSecurity: Boolean = false
)
