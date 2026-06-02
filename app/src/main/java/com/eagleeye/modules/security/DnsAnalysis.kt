package com.eagleeye.modules.security

/**
 * Pure helpers extracted from [ThreatDetector] for DNS classification. Kept at
 * the top level so they can be unit-tested without any Android dependencies.
 */

/**
 * Converts a little-endian packed IPv4 address (as returned by `DhcpInfo.dns1`)
 * to dotted-quad notation.
 */
fun intToIp(ip: Int): String =
    "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"

/**
 * `true` if the address is not in an RFC1918 private range
 * (10/8, 172.16/12, 192.168/16). Malformed input returns `false`.
 */
fun isPublicDns(ip: String): Boolean {
    val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size != 4) return false
    return !(parts[0] == 10 ||
            (parts[0] == 172 && parts[1] in 16..31) ||
            (parts[0] == 192 && parts[1] == 168))
}

/**
 * Recognises well-known public resolvers (Google, Cloudflare, Quad9, OpenDNS).
 */
fun isKnownGoodDns(ip: String): Boolean = ip in KNOWN_GOOD_DNS

private val KNOWN_GOOD_DNS = setOf(
    "8.8.8.8", "8.8.4.4",            // Google
    "1.1.1.1", "1.0.0.1",            // Cloudflare
    "9.9.9.9", "149.112.112.112",    // Quad9
    "208.67.222.222", "208.67.220.220" // OpenDNS
)

/** Minimal description of a scan result for evil-twin analysis. */
data class ScanInfo(val ssid: String, val bssid: String)

/**
 * Returns the BSSIDs broadcasting the same SSID as the currently-connected AP
 * but with a different BSSID. A non-empty list is the signature of an
 * evil-twin / rogue AP.
 */
fun findEvilTwinBssids(
    scans: List<ScanInfo>,
    currentSsid: String,
    currentBssid: String
): List<String> {
    if (currentSsid.isBlank() || currentBssid.isBlank()) return emptyList()
    return scans
        .filter { it.ssid.equals(currentSsid, ignoreCase = true) && it.bssid != currentBssid }
        .map { it.bssid }
}
