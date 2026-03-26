package com.eagleeye.data

data class PingResult(
    val host: String,
    val sent: Int,
    val received: Int,
    val lostPercent: Int,
    val minMs: Long,
    val avgMs: Long,
    val maxMs: Long,
    val jitterMs: Long,
    val samples: List<Long>       // -1 = timeout
) {
    val packetLoss: Boolean get() = lostPercent > 0
    val quality: String get() = when {
        lostPercent == 0 && avgMs < 10 -> "Excellent"
        lostPercent == 0 && avgMs < 50 -> "Good"
        lostPercent < 10 && avgMs < 100 -> "Fair"
        lostPercent < 30 -> "Poor"
        else -> "Very Poor"
    }
}

data class TracerouteHop(
    val index: Int,
    val ip: String,
    val hostname: String,
    val latencyMs: Long,        // -1 = timeout (*)
    val isTimeout: Boolean = latencyMs < 0
)

data class PortScanResult(
    val host: String,
    val port: Int,
    val isOpen: Boolean,
    val service: String,
    val banner: String = ""
)

data class DnsResult(
    val query: String,
    val type: String,           // A, AAAA, PTR, MX
    val answers: List<String>,
    val resolvedIn: Long,
    val dnsServer: String
)

data class PublicIpInfo(
    val ipv4: String,
    val ipv6: String,
    val localIp: String,
    val gateway: String
)
