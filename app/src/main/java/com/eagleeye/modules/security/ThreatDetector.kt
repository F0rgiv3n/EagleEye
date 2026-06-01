package com.eagleeye.modules.security

import android.content.Context
import android.net.wifi.WifiManager
import com.eagleeye.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Core security analysis engine.
 * Each detector is independent — failures in one don't affect others.
 */
class ThreatDetector(private val context: Context) {

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    // Baseline DNS: stored on first run, checked on subsequent runs
    private var baselineDns1: String? = null
    private var baselineDns2: String? = null

    // ARP baseline: IP → MAC, used to detect MAC changes (ARP spoof)
    private val arpBaseline = mutableMapOf<String, String>()

    suspend fun runFullAudit(knownDeviceCount: Int): SecurityScore = withContext(Dispatchers.IO) {
        val threats = mutableListOf<Threat>()

        // --- 1. Encryption check ---
        val encryptionResult = checkEncryption()
        threats.addAll(encryptionResult.threats)

        // --- 2. WPS check ---
        val wpsResult = checkWps()
        threats.addAll(wpsResult.threats)

        // --- 3. Evil twin detection ---
        val evilTwinResult = checkEvilTwin()
        threats.addAll(evilTwinResult.threats)

        // --- 4. DNS integrity ---
        val dnsResult = checkDnsIntegrity()
        threats.addAll(dnsResult.threats)

        // --- 5. ARP spoofing ---
        val arpResult = checkArpSpoofing()
        threats.addAll(arpResult.threats)

        // --- 6. Open / weak network extras ---
        val extraThreats = checkAdditionalThreats()
        threats.addAll(extraThreats)

        // --- Score calculation ---
        val encScore = encryptionResult.score      // 0-30
        val wpsScore = if (wpsResult.threats.isEmpty()) 15 else 0
        val evilTwinScore = if (evilTwinResult.threats.isEmpty()) 15 else 0
        val dnsScore = if (dnsResult.threats.isEmpty()) 10 else 0
        val unknownScore = if (knownDeviceCount == 0) 15 else 0
        val arpScore = if (arpResult.threats.isEmpty()) 15 else 0

        val total = (encScore + wpsScore + evilTwinScore + dnsScore + unknownScore + arpScore).coerceIn(0, 100)

        SecurityScore(
            total = total,
            encryption = encScore,
            noWps = wpsScore,
            noEvilTwin = evilTwinScore,
            dnsIntegrity = dnsScore,
            noUnknownDevices = unknownScore,
            noOpenPorts = arpScore,
            threats = threats.sortedByDescending { it.level.ordinal.inv() }
        )
    }

    // ── Encryption ──────────────────────────────────────────────────────────

    private data class CheckResult(val score: Int = 0, val threats: List<Threat> = emptyList())

    @Suppress("DEPRECATION")
    private fun checkEncryption(): CheckResult {
        val scanResult = getCurrentScanResult() ?: return CheckResult(score = 20)
        val caps = scanResult.capabilities

        return when {
            caps.contains("WPA3") -> CheckResult(score = 30)
            caps.contains("WPA2") -> CheckResult(score = 20)
            caps.contains("WPA") -> CheckResult(
                score = 10,
                threats = listOf(Threat(
                    id = "weak_enc",
                    level = ThreatLevel.MEDIUM,
                    title = "Weak Encryption (WPA)",
                    description = "Network uses WPA (TKIP). WPA has known vulnerabilities and can be cracked.",
                    recommendation = "Upgrade router to WPA2 or WPA3 in router settings."
                ))
            )
            caps.contains("WEP") -> CheckResult(
                score = 0,
                threats = listOf(Threat(
                    id = "wep",
                    level = ThreatLevel.CRITICAL,
                    title = "WEP Encryption Detected",
                    description = "WEP is broken and can be cracked in minutes with freely available tools.",
                    recommendation = "Immediately change router encryption to WPA2/WPA3."
                ))
            )
            !caps.contains("WPA") -> CheckResult(
                score = 0,
                threats = listOf(Threat(
                    id = "open_net",
                    level = ThreatLevel.CRITICAL,
                    title = "Open Network — No Encryption",
                    description = "This network has no encryption. All traffic is visible to anyone nearby.",
                    recommendation = "Avoid sending sensitive data. Use a VPN on this network."
                ))
            )
            else -> CheckResult(score = 20)
        }
    }

    // ── WPS ──────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun checkWps(): CheckResult {
        val caps = getCurrentScanResult()?.capabilities ?: return CheckResult()
        return if (caps.contains("WPS")) {
            CheckResult(threats = listOf(Threat(
                id = "wps_enabled",
                level = ThreatLevel.HIGH,
                title = "WPS Enabled",
                description = "Wi-Fi Protected Setup (WPS) is enabled. WPS PIN attacks (Pixie Dust) can compromise the router.",
                recommendation = "Disable WPS in your router admin panel."
            )))
        } else CheckResult()
    }

    // ── Evil Twin ────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun checkEvilTwin(): CheckResult {
        val currentInfo = wifiManager.connectionInfo ?: return CheckResult()
        val currentSsid = currentInfo.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: return CheckResult()
        val currentBssid = currentInfo.bssid ?: return CheckResult()

        val scanResults = try { wifiManager.scanResults } catch (e: SecurityException) { null }
            ?: return CheckResult()

        val duplicates = scanResults.filter { result ->
            val ssid = result.SSID?.removePrefix("\"")?.removeSuffix("\"") ?: ""
            ssid.equals(currentSsid, ignoreCase = true) && result.BSSID != currentBssid
        }

        return if (duplicates.isNotEmpty()) {
            val bssids = duplicates.joinToString(", ") { it.BSSID ?: "?" }
            CheckResult(threats = listOf(Threat(
                id = "evil_twin",
                level = ThreatLevel.CRITICAL,
                title = "Evil Twin AP Detected",
                description = "Found ${duplicates.size} other access point(s) broadcasting SSID \"$currentSsid\" with different BSSIDs: $bssids. This may be a rogue AP mimicking your network.",
                recommendation = "Disconnect immediately and verify the legitimacy of your network. Contact your network admin."
            )))
        } else CheckResult()
    }

    // ── DNS Integrity ────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun checkDnsIntegrity(): CheckResult {
        val dhcp = wifiManager.dhcpInfo ?: return CheckResult()
        val dns1 = intToIp(dhcp.dns1)
        val dns2 = intToIp(dhcp.dns2)

        // Initialize baseline on first run
        if (baselineDns1 == null) {
            baselineDns1 = dns1
            baselineDns2 = dns2
            return CheckResult()
        }

        val threats = mutableListOf<Threat>()

        if (dns1 != baselineDns1) {
            threats.add(Threat(
                id = "dns_changed",
                level = ThreatLevel.HIGH,
                title = "DNS Server Changed",
                description = "Primary DNS changed from $baselineDns1 to $dns1. This may indicate DNS hijacking or router compromise.",
                recommendation = "Verify DNS settings in router. Consider using hardcoded DNS (8.8.8.8 or 1.1.1.1)."
            ))
            baselineDns1 = dns1
        }

        // Check for suspicious DNS (non-standard, private range unexpected)
        if (isPublicDns(dns1) && !isKnownGoodDns(dns1)) {
            threats.add(Threat(
                id = "unknown_dns",
                level = ThreatLevel.MEDIUM,
                title = "Unknown DNS Server",
                description = "DNS server $dns1 is not a well-known provider. Unknown DNS servers can intercept your queries.",
                recommendation = "Verify your DNS settings. Consider using 1.1.1.1 (Cloudflare) or 8.8.8.8 (Google)."
            ))
        }

        return CheckResult(threats = threats)
    }

    // ── ARP Spoofing ─────────────────────────────────────────────────────────

    private fun checkArpSpoofing(): CheckResult {
        val arpTable = readArpTable()
        val threats = mutableListOf<Threat>()

        // Detect: same IP → different MAC than baseline
        for (entry in arpTable) {
            val previousMac = arpBaseline[entry.ip]
            if (previousMac != null && previousMac != entry.mac && entry.mac != "00:00:00:00:00:00") {
                threats.add(Threat(
                    id = "arp_spoof_${entry.ip}",
                    level = ThreatLevel.CRITICAL,
                    title = "ARP Spoofing Detected",
                    description = "IP ${entry.ip} changed MAC from $previousMac to ${entry.mac}. This is a classic sign of an ARP spoofing/MITM attack.",
                    recommendation = "Disconnect immediately. An attacker may be intercepting your traffic."
                ))
            }
            arpBaseline[entry.ip] = entry.mac
        }

        // Detect: multiple IPs claiming same MAC (gateway MAC conflict)
        val macToIps = arpTable.groupBy { it.mac }
        for ((mac, entries) in macToIps) {
            if (entries.size > 1 && mac != "00:00:00:00:00:00") {
                val ips = entries.joinToString(", ") { it.ip }
                threats.add(Threat(
                    id = "arp_conflict_$mac",
                    level = ThreatLevel.HIGH,
                    title = "ARP MAC Conflict",
                    description = "MAC $mac is assigned to multiple IPs: $ips. Possible ARP poisoning or MITM.",
                    recommendation = "Check your network for rogue devices. Clear ARP cache and monitor."
                ))
            }
        }

        return CheckResult(threats = threats)
    }

    // ── Additional Checks ────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun checkAdditionalThreats(): List<Threat> {
        val threats = mutableListOf<Threat>()
        val scanResults = try { wifiManager.scanResults } catch (e: SecurityException) { null }
            ?: return threats
        val currentBssid = wifiManager.connectionInfo?.bssid

        // Hidden SSID warning
        val connectedHidden = scanResults.firstOrNull {
            it.BSSID == currentBssid && it.SSID.isNullOrEmpty()
        }
        if (connectedHidden != null) {
            threats.add(Threat(
                id = "hidden_ssid",
                level = ThreatLevel.LOW,
                title = "Connected to Hidden SSID",
                description = "Your device is connected to a hidden network. Hidden SSIDs offer no real security and can make your device broadcast probe requests.",
                recommendation = "Prefer networks with visible SSIDs."
            ))
        }

        // Multiple open networks with same SSID
        val openNetworks = scanResults.filter {
            !it.capabilities.contains("WPA") && !it.capabilities.contains("WEP")
        }
        if (openNetworks.size > 1) {
            val ssids = openNetworks.map { it.SSID ?: "<hidden>" }.distinct().take(3).joinToString(", ")
            threats.add(Threat(
                id = "multiple_open",
                level = ThreatLevel.INFO,
                title = "${openNetworks.size} Open Networks Nearby",
                description = "Detected ${openNetworks.size} unencrypted networks nearby: $ssids. Connecting to these exposes all traffic.",
                recommendation = "Avoid connecting to open networks without a VPN."
            ))
        }

        return threats
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun getCurrentScanResult() = try {
        wifiManager.scanResults?.firstOrNull { it.BSSID == wifiManager.connectionInfo?.bssid }
    } catch (e: SecurityException) { null }

    private fun readArpTable(): List<ArpEntry> {
        return try {
            java.io.File("/proc/net/arp").bufferedReader().useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 4 && parts[2] == "0x2") {
                        ArpEntry(ip = parts[0], mac = parts[3].uppercase())
                    } else null
                }.toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun intToIp(ip: Int): String =
        "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"

    private fun isPublicDns(ip: String): Boolean {
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return !(parts[0] == 10 || (parts[0] == 172 && parts[1] in 16..31) ||
                (parts[0] == 192 && parts[1] == 168))
    }

    private fun isKnownGoodDns(ip: String) = ip in setOf(
        "8.8.8.8", "8.8.4.4",       // Google
        "1.1.1.1", "1.0.0.1",       // Cloudflare
        "9.9.9.9", "149.112.112.112", // Quad9
        "208.67.222.222", "208.67.220.220" // OpenDNS
    )
}
