package com.eagleeye.data

// ── CVE ──────────────────────────────────────────────────────────────────────

data class CveEntry(
    val id: String,             // CVE-2023-XXXX
    val description: String,
    val cvssScore: Float,       // 0.0 - 10.0
    val severity: CveSeverity,
    val publishedDate: String,
    val references: List<String>
)

enum class CveSeverity { CRITICAL, HIGH, MEDIUM, LOW, NONE }

data class CveLookupResult(
    val query: String,
    val entries: List<CveEntry>,
    val totalFound: Int,
    val error: String? = null
)

// ── SSL/TLS ───────────────────────────────────────────────────────────────────

data class SslCertInfo(
    val host: String,
    val port: Int,
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validUntil: String,
    val isExpired: Boolean,
    val daysUntilExpiry: Long,
    val isSelfSigned: Boolean,
    val protocol: String,       // TLSv1.2, TLSv1.3
    val cipherSuite: String,
    val isWeak: Boolean,
    val grade: SslGrade,
    val error: String? = null
)

enum class SslGrade { A_PLUS, A, B, C, F, ERROR }

// ── VPN Leak ──────────────────────────────────────────────────────────────────

data class VpnLeakResult(
    val isVpnActive: Boolean,
    val publicIpWithVpn: String,
    val dnsServers: List<String>,
    val dnsLeakDetected: Boolean,
    val ipv6LeakDetected: Boolean,
    val ipv6Address: String,
    val leakDetails: List<String>
)

// ── Export ────────────────────────────────────────────────────────────────────

data class FullReport(
    val timestamp: Long,
    val deviceModel: String,
    val wifiSsid: String,
    val securityScore: Int,
    val securityGrade: String,
    val threats: List<String>,
    val lanDevices: List<String>,
    val openPorts: Map<String, List<Int>>
)
