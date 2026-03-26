package com.eagleeye.data

// ── Captive Portal ────────────────────────────────────────────────────────────

enum class PortalStatus { NONE, DETECTED, SUSPICIOUS, ERROR }

data class CaptivePortalResult(
    val status: PortalStatus,
    val portalUrl: String       = "",
    val redirectChain: List<String> = emptyList(),
    val pageTitle: String       = "",
    val hasCertIssue: Boolean   = false,
    val isSuspicious: Boolean   = false,
    val suspicionReasons: List<String> = emptyList(),
    val responseCode: Int       = 0,
    val checkedUrl: String      = "",
    val error: String?          = null
)

// ── IoT Device ────────────────────────────────────────────────────────────────

enum class DeviceCategory {
    ROUTER, CAMERA, SMART_TV, PRINTER, PHONE, PC,
    NAS, GAME_CONSOLE, SMART_SPEAKER, IOT_SENSOR,
    UNKNOWN
}

data class IoTProfile(
    val ip: String,
    val mac: String,
    val category: DeviceCategory,
    val deviceModel: String,        // best guess model name
    val firmwareHint: String,       // from HTTP banner
    val hasAdminPanel: Boolean,
    val adminPort: Int,
    val hasDefaultCredentials: Boolean,
    val defaultCreds: List<Pair<String, String>>,   // list of (user, pass)
    val services: List<String>,     // from mDNS/SSDP
    val riskLevel: IoTRisk
)

enum class IoTRisk { HIGH, MEDIUM, LOW, SAFE }

data class SsdpDevice(
    val ip: String,
    val server: String,
    val location: String,           // URL to device description XML
    val usn: String,                // Unique Service Name
    val st: String                  // Search Target
)
