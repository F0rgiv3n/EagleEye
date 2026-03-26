package com.eagleeye.data

// ── HTTP Security Headers ──────────────────────────────────────────────────

enum class HeaderGrade { A_PLUS, A, B, C, F, ERROR }
enum class HeaderStatus { PRESENT, MISSING, WEAK }

data class SecurityHeaderEntry(
    val name: String,
    val value: String,
    val status: HeaderStatus,
    val points: Int,
    val description: String
)

data class HttpHeadersResult(
    val url: String,
    val grade: HeaderGrade,
    val score: Int,
    val headers: List<SecurityHeaderEntry>,
    val infoLeaks: List<String>,
    val responseCode: Int = 0,
    val error: String? = null
)

// ── Threat Intelligence ────────────────────────────────────────────────────

data class ThreatIntelResult(
    val ip: String,
    val country: String = "",
    val countryCode: String = "",
    val city: String = "",
    val isp: String = "",
    val org: String = "",
    val asn: String = "",
    val isProxy: Boolean = false,
    val isHosting: Boolean = false,
    val riskLevel: String = "UNKNOWN",
    val riskReasons: List<String> = emptyList(),
    val abuseScore: Int = -1,
    val abuseReports: Int = 0,
    val abuseCategories: List<String> = emptyList(),
    val error: String? = null
)

// ── Shodan InternetDB ──────────────────────────────────────────────────────

data class ShodanResult(
    val ip: String,
    val hostnames: List<String> = emptyList(),
    val ports: List<Int> = emptyList(),
    val cves: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val vulns: List<String> = emptyList(),
    val error: String? = null
)
