package com.eagleeye.data

// ── Speed Test ───────────────────────────────────────────────────────────────

enum class SpeedPhase { IDLE, PINGING, DOWNLOADING, UPLOADING, DONE }

data class SpeedTestProgress(
    val phase: SpeedPhase = SpeedPhase.IDLE,
    val progress: Float = 0f,
    val currentMbps: Float = 0f
)

data class SpeedTestResult(
    val downloadMbps: Float = 0f,
    val uploadMbps: Float = 0f,
    val pingMs: Long = -1L,
    val jitterMs: Long = 0L,
    val server: String = "speed.cloudflare.com",
    val error: String? = null
)

// ── Bandwidth Monitor ────────────────────────────────────────────────────────

data class BandwidthSample(
    val timestamp: Long = System.currentTimeMillis(),
    val rxSpeed: Float = 0f,   // bytes/sec
    val txSpeed: Float = 0f
)

// ── mDNS Service Discovery ───────────────────────────────────────────────────

data class MdnsService(
    val name: String,
    val type: String,
    val host: String = "",
    val port: Int = 0,
    val ip: String = ""
) {
    val friendlyType: String get() = when {
        type.contains("googlecast", ignoreCase = true) -> "Chromecast"
        type.contains("airplay",    ignoreCase = true) -> "AirPlay"
        type.contains("raop",       ignoreCase = true) -> "AirPlay Audio"
        type.contains("ipp",        ignoreCase = true) ||
        type.contains("printer",    ignoreCase = true) -> "Printer"
        type.contains("smb",        ignoreCase = true) -> "File Share"
        type.contains("ssh",        ignoreCase = true) -> "SSH"
        type.contains("ftp",        ignoreCase = true) -> "FTP"
        type.contains("plex",       ignoreCase = true) -> "Plex Media"
        type.contains("spotify",    ignoreCase = true) -> "Spotify Connect"
        type.contains("daap",       ignoreCase = true) -> "iTunes Share"
        type.contains("http",       ignoreCase = true) -> "HTTP Service"
        else -> type.trimStart('_').substringBefore('.')
    }
}

// ── ARP Cache ────────────────────────────────────────────────────────────────

data class ArpCacheEntry(
    val ip: String,
    val mac: String,
    val flags: String,
    val iface: String,
    val isComplete: Boolean    // flags == "0x2"
)

// ── IPv6 Inspector ───────────────────────────────────────────────────────────

data class IPv6AddressInfo(
    val address: String,
    val type: String,          // "Link-local" | "Global" | "Unique-local" | "Loopback"
    val iface: String,
    val prefixLength: Int = 0
)

data class IPv6InspectorResult(
    val addresses: List<IPv6AddressInfo> = emptyList(),
    val hasGlobalAddress: Boolean = false,
    val hasConnectivity: Boolean = false,
    val globalAddress: String = "",
    val linkLocalAddress: String = "",
    val note: String = "",
    val error: String? = null
)

// ── DNS Benchmark ────────────────────────────────────────────────────────────

data class DnsServerResult(
    val server: String,
    val name: String,
    val avgMs: Long,
    val minMs: Long,
    val maxMs: Long,
    val successRate: Int,      // 0-100
    val rank: Int = 0
)

data class DnsBenchmarkResult(
    val results: List<DnsServerResult> = emptyList(),
    val fastest: String = "",
    val error: String? = null
)

// ── Network Interfaces ───────────────────────────────────────────────────────

data class NetworkInterfaceInfo(
    val name: String,
    val friendlyName: String,
    val type: String,          // "Wi-Fi" | "Cellular" | "VPN" | "Loopback" | "Ethernet" | "Other"
    val ipv4: String = "",
    val ipv6: String = "",
    val mac: String = "",
    val mtu: Int = 0,
    val isUp: Boolean = false,
    val isLoopback: Boolean = false
)

// ── Firewall Tester ──────────────────────────────────────────────────────────

data class PortBlockResult(
    val port: Int,
    val service: String,
    val isOpen: Boolean,
    val latencyMs: Long = -1L
)

data class FirewallTestResult(
    val results: List<PortBlockResult> = emptyList(),
    val openCount: Int = 0,
    val blockedCount: Int = 0,
    val testHost: String = "portquiz.net",
    val error: String? = null
)

// ── Signal History ───────────────────────────────────────────────────────────

data class SignalSample(
    val timestamp: Long = System.currentTimeMillis(),
    val rssi: Int,
    val ssid: String
)
