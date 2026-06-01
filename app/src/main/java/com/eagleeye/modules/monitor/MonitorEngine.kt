package com.eagleeye.modules.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.eagleeye.data.*
import com.eagleeye.data.db.AppDatabase
import com.eagleeye.modules.lan.OuiLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Runs a single monitoring cycle:
 * - Compares current ARP table against known devices → new device / device gone
 * - Checks for evil twin (same SSID, different BSSID)
 * - Monitors ARP table for MAC conflicts (ARP spoof)
 * - Checks DNS baseline
 *
 * Called by MonitorService on each scheduled cycle.
 */
class MonitorEngine(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // In-memory baselines (reset when service restarts)
    private val arpBaseline   = mutableMapOf<String, String>()   // ip → mac
    private val knownIps      = mutableSetOf<String>()
    private var baselineDns1  = ""
    private var baselineDns2  = ""
    private var baselineBssid = ""
    private var arpFallbackNotified = false

    suspend fun runCycle(config: MonitorConfig): List<NetworkEvent> {
        val events = mutableListOf<NetworkEvent>()

        if (!isConnected()) return events

        OuiLookup.load(context)

        val currentSsid  = getCurrentSsid()
        val currentBssid = getCurrentBssid()

        // ── 1. ARP / new device detection ────────────────────────────────────
        // /proc/net/arp is restricted on Android 11+ — readArpTable() returns
        // empty there. Fall back to a ping sweep that produces IP-only entries.
        var arpEntries = readArpTable()
        val arpUnavailable = arpEntries.isEmpty()
        if (arpUnavailable) {
            arpEntries = pingSweep()
            if (!arpFallbackNotified && config.notifyNewDevice) {
                arpFallbackNotified = true
                events.add(NetworkEvent(
                    type = EventType.SCAN_COMPLETE,
                    severity = EventSeverity.INFO,
                    title = "ARP Detection Limited",
                    detail = "Android 11+ restricts /proc/net/arp. Using ping sweep — new IPs detected, but ARP-spoof and MAC-conflict checks are disabled.",
                    ssid = currentSsid
                ))
            }
        }

        for (entry in arpEntries) {
            val ip  = entry.ip
            val mac = entry.mac
            val hasMac = mac.isNotBlank() && mac != "00:00:00:00:00:00"

            // New device
            if (ip !in knownIps) {
                knownIps.add(ip)
                val lookupKey = if (hasMac) mac else ip
                val existing = db.lanDeviceDao().getByMac(lookupKey)
                if (existing == null && config.notifyNewDevice) {
                    val vendor = if (hasMac) OuiLookup.lookup(mac) else "Unknown (no ARP access)"
                    val macLabel = if (hasMac) mac else "MAC unavailable"
                    events.add(NetworkEvent(
                        type = EventType.NEW_DEVICE,
                        severity = EventSeverity.MEDIUM,
                        title = "New Device on LAN",
                        detail = "Unknown device appeared: $ip ($macLabel) — Vendor: $vendor",
                        ssid = currentSsid, ip = ip, mac = if (hasMac) mac else ""
                    ))
                }
            }

            // ARP spoof: requires a real MAC. Skip on Android 11+ fallback.
            if (config.notifyArpSpoof && hasMac) {
                val prevMac = arpBaseline[ip]
                if (prevMac != null && prevMac != mac) {
                    events.add(NetworkEvent(
                        type = EventType.ARP_SPOOF,
                        severity = EventSeverity.CRITICAL,
                        title = "ARP Spoofing Detected!",
                        detail = "$ip changed MAC: $prevMac → $mac. Possible MITM attack.",
                        ssid = currentSsid, ip = ip, mac = mac
                    ))
                }
                arpBaseline[ip] = mac
            }
        }

        // ── 2. MAC conflict (multiple IPs same MAC = gateway poisoning) ──────
        // Only meaningful when we have real MACs.
        if (config.notifyArpSpoof && !arpUnavailable) {
            val macToIps = arpEntries
                .filter { it.mac.isNotBlank() && it.mac != "00:00:00:00:00:00" }
                .groupBy { it.mac }
            for ((mac, entries) in macToIps) {
                if (entries.size > 1) {
                    val ips = entries.joinToString(", ") { it.ip }
                    events.add(NetworkEvent(
                        type = EventType.ARP_SPOOF,
                        severity = EventSeverity.HIGH,
                        title = "ARP MAC Conflict",
                        detail = "MAC $mac claimed by ${entries.size} IPs: $ips",
                        ssid = currentSsid, mac = mac
                    ))
                }
            }
        }

        // ── 3. Evil twin ─────────────────────────────────────────────────────
        if (config.notifyEvilTwin && currentBssid.isNotEmpty()) {
            @Suppress("DEPRECATION")
            val scans = try { wifiManager.scanResults } catch (e: SecurityException) { null }
                ?: emptyList()
            val twins = scans.filter { r ->
                val ssid = r.SSID?.removePrefix("\"")?.removeSuffix("\"") ?: ""
                ssid.equals(currentSsid, ignoreCase = true) && r.BSSID != currentBssid
            }
            if (twins.isNotEmpty()) {
                val bssids = twins.joinToString(", ") { it.BSSID ?: "?" }
                events.add(NetworkEvent(
                    type = EventType.EVIL_TWIN,
                    severity = EventSeverity.CRITICAL,
                    title = "Evil Twin AP Detected!",
                    detail = "\"$currentSsid\" broadcast by foreign BSSID(s): $bssids",
                    ssid = currentSsid
                ))
            }
            baselineBssid = currentBssid
        }

        // ── 4. DNS change ────────────────────────────────────────────────────
        if (config.notifyDnsChange) {
            @Suppress("DEPRECATION")
            val dhcp = wifiManager.dhcpInfo
            val dns1 = intToIp(dhcp?.dns1 ?: 0)
            val dns2 = intToIp(dhcp?.dns2 ?: 0)

            if (baselineDns1.isEmpty()) {
                baselineDns1 = dns1; baselineDns2 = dns2
            } else if (dns1 != baselineDns1 && dns1 != "0.0.0.0") {
                events.add(NetworkEvent(
                    type = EventType.DNS_CHANGED,
                    severity = EventSeverity.HIGH,
                    title = "DNS Server Changed",
                    detail = "Primary DNS changed: $baselineDns1 → $dns1. Possible DNS hijacking.",
                    ssid = currentSsid
                ))
                baselineDns1 = dns1
            }
        }

        // ── 5. SCAN_COMPLETE event ────────────────────────────────────────────
        events.add(NetworkEvent(
            type = EventType.SCAN_COMPLETE,
            severity = EventSeverity.INFO,
            title = "Scan Completed",
            detail = "Monitored ${arpEntries.size} devices on $currentSsid. Found ${events.size} issue(s).",
            ssid = currentSsid
        ))

        // Persist all events
        events.forEach { db.networkEventDao().insert(it) }

        // Clean up events older than 7 days
        db.networkEventDao().deleteOlderThan(System.currentTimeMillis() - 7 * 86400_000L)

        return events
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isConnected(): Boolean {
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @Suppress("DEPRECATION")
    private fun getCurrentSsid() = wifiManager.connectionInfo?.ssid
        ?.removePrefix("\"")?.removeSuffix("\"") ?: ""

    @Suppress("DEPRECATION")
    private fun getCurrentBssid() = wifiManager.connectionInfo?.bssid ?: ""

    private fun readArpTable(): List<ArpEntry> {
        return try {
            java.io.File("/proc/net/arp").bufferedReader().useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size < 4) return@mapNotNull null
                    val mac = parts[3].uppercase()
                    // Android 11+ returns rows but with zeroed MACs — treat as no data.
                    if (mac == "00:00:00:00:00:00") null else ArpEntry(parts[0], mac)
                }.toList()
            }
        } catch (e: Exception) { emptyList() }
    }

    @Suppress("DEPRECATION")
    private suspend fun pingSweep(): List<ArpEntry> {
        val subnet = getSubnetPrefix() ?: return emptyList()
        return coroutineScope {
            (1..254).map { last ->
                async(Dispatchers.IO) {
                    val ip = "$subnet.$last"
                    val reachable = try {
                        InetAddress.getByName(ip).isReachable(200)
                    } catch (_: Exception) { false }
                    if (reachable) ArpEntry(ip = ip, mac = "") else null
                }
            }.awaitAll().filterNotNull()
        }
    }

    @Suppress("DEPRECATION")
    private fun getSubnetPrefix(): String? {
        val gw = wifiManager.dhcpInfo?.gateway ?: 0
        if (gw == 0) return null
        return "${gw and 0xFF}.${(gw shr 8) and 0xFF}.${(gw shr 16) and 0xFF}"
    }

    private fun intToIp(ip: Int) =
        "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
}
