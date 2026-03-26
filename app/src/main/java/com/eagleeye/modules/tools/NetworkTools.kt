package com.eagleeye.modules.tools

import android.content.Context
import android.net.wifi.WifiManager
import com.eagleeye.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.*

class NetworkTools(private val context: Context) {

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    // ── Ping ──────────────────────────────────────────────────────────────────

    suspend fun ping(host: String, count: Int = 8, timeoutMs: Int = 1000): PingResult =
        withContext(Dispatchers.IO) {
            val samples = mutableListOf<Long>()
            var received = 0

            repeat(count) {
                val latency = pingOnce(host, timeoutMs)
                samples.add(latency)
                if (latency >= 0) received++
            }

            val valid = samples.filter { it >= 0 }
            val lost = count - received
            val lostPct = (lost * 100) / count

            if (valid.isEmpty()) {
                return@withContext PingResult(
                    host = host, sent = count, received = 0,
                    lostPercent = 100, minMs = -1, avgMs = -1, maxMs = -1,
                    jitterMs = -1, samples = samples
                )
            }

            val min = valid.min()
            val max = valid.max()
            val avg = valid.sum() / valid.size
            val jitter = if (valid.size > 1) {
                valid.zipWithNext { a, b -> kotlin.math.abs(b - a) }.average().toLong()
            } else 0L

            PingResult(
                host = host, sent = count, received = received,
                lostPercent = lostPct, minMs = min, avgMs = avg, maxMs = max,
                jitterMs = jitter, samples = samples
            )
        }

    private fun pingOnce(host: String, timeoutMs: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(host).isReachable(timeoutMs)
            if (reachable) System.currentTimeMillis() - start else -1L
        } catch (e: Exception) { -1L }
    }

    // ── Traceroute ────────────────────────────────────────────────────────────

    suspend fun traceroute(host: String, maxHops: Int = 30): List<TracerouteHop> =
        withContext(Dispatchers.IO) {
            val hops = mutableListOf<TracerouteHop>()
            // Validate host to prevent command injection
            if (!host.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
                return@withContext listOf(TracerouteHop(1, "*", "Invalid host", -1))
            }
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("traceroute", "-m", "$maxHops", "-w", "1", "-q", "1", host)
                )
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                var hopIndex = 0

                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: continue
                    if (l.contains("traceroute to")) continue

                    val hop = parseTracerouteLine(l, ++hopIndex)
                    if (hop != null) {
                        hops.add(hop)
                        if (hop.ip == resolveHost(host)) break  // reached destination
                    }
                }
                process.destroy()
            } catch (e: Exception) {
                // Fallback: manual TTL-based traceroute via ICMP
                hops.addAll(manualTraceroute(host, maxHops))
            }
            hops
        }

    private fun parseTracerouteLine(line: String, index: Int): TracerouteHop? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || !trimmed[0].isDigit()) return null

        return if (trimmed.contains("* * *") || trimmed.endsWith("*")) {
            TracerouteHop(index = index, ip = "*", hostname = "", latencyMs = -1)
        } else {
            val parts = trimmed.split("\\s+".toRegex())
            val ip = parts.firstOrNull { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) } ?: "*"
            val hostname = parts.getOrNull(1)?.let {
                if (it != ip && !it.startsWith("(")) it else ""
            } ?: ""
            val latency = parts.firstOrNull { it.endsWith("ms") }
                ?.replace("ms", "")?.toLongOrNull() ?: -1L
            TracerouteHop(index = index, ip = ip, hostname = hostname, latencyMs = latency)
        }
    }

    private fun manualTraceroute(host: String, maxHops: Int): List<TracerouteHop> {
        val hops = mutableListOf<TracerouteHop>()
        val destIp = try { InetAddress.getByName(host).hostAddress ?: host } catch (e: Exception) { host }

        for (ttl in 1..maxHops) {
            val start = System.currentTimeMillis()
            try {
                val socket = InetAddress.getByName(host)
                // Use isReachable with short timeout per hop — not TTL-accurate but functional fallback
                val reachable = socket.isReachable(800)
                val latency = System.currentTimeMillis() - start
                if (reachable) {
                    hops.add(TracerouteHop(ttl, destIp, host, latency))
                    break
                } else {
                    hops.add(TracerouteHop(ttl, "*", "", -1))
                }
            } catch (e: Exception) {
                hops.add(TracerouteHop(ttl, "*", "", -1))
            }
        }
        return hops
    }

    private fun resolveHost(host: String): String =
        try { InetAddress.getByName(host).hostAddress ?: host } catch (e: Exception) { host }

    // ── Port Scanner ──────────────────────────────────────────────────────────

    suspend fun scanPorts(
        host: String,
        ports: List<Int>,
        timeoutMs: Int = 300,
        onProgress: (Int) -> Unit = {}
    ): List<PortScanResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PortScanResult>()
        ports.forEachIndexed { idx, port ->
            val result = scanPort(host, port, timeoutMs)
            results.add(result)
            onProgress(idx + 1)
        }
        results.filter { it.isOpen }
    }

    private fun scanPort(host: String, port: Int, timeoutMs: Int): PortScanResult {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val banner = grabBanner(socket)
                PortScanResult(host, port, true, getServiceName(port), banner)
            }
        } catch (e: Exception) {
            PortScanResult(host, port, false, getServiceName(port))
        }
    }

    private fun grabBanner(socket: Socket): String {
        return try {
            socket.soTimeout = 300
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            reader.readLine()?.take(100) ?: ""
        } catch (e: Exception) { "" }
    }

    // ── DNS Lookup ────────────────────────────────────────────────────────────

    suspend fun dnsLookup(query: String): DnsResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val dnsServer = getDnsServer()

        return@withContext try {
            val isIp = query.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))
            if (isIp) {
                // Reverse DNS
                val addr = InetAddress.getByName(query)
                val hostname = addr.canonicalHostName
                DnsResult(
                    query = query,
                    type = "PTR",
                    answers = listOf(hostname),
                    resolvedIn = System.currentTimeMillis() - start,
                    dnsServer = dnsServer
                )
            } else {
                // Forward A + AAAA
                val addresses = InetAddress.getAllByName(query)
                val ipv4 = addresses.filter { it is Inet4Address }.map { it.hostAddress ?: "" }
                val ipv6 = addresses.filter { it is Inet6Address }.map { it.hostAddress ?: "" }
                val all = (ipv4 + ipv6).filter { it.isNotEmpty() }
                DnsResult(
                    query = query,
                    type = if (ipv6.isNotEmpty()) "A + AAAA" else "A",
                    answers = all,
                    resolvedIn = System.currentTimeMillis() - start,
                    dnsServer = dnsServer
                )
            }
        } catch (e: Exception) {
            DnsResult(query, "ERROR", listOf(e.message ?: "Resolution failed"), -1, dnsServer)
        }
    }

    // ── Public IP ─────────────────────────────────────────────────────────────

    suspend fun getPublicIpInfo(): PublicIpInfo = withContext(Dispatchers.IO) {
        val ipv4 = fetchPublicIpv4()
        val ipv6 = fetchPublicIpv6()
        val (localIp, gateway) = getLocalInfo()
        PublicIpInfo(ipv4, ipv6, localIp, gateway)
    }

    private fun fetchPublicIpv4(): String {
        val conn1 = URL("https://api4.my-ip.io/v2/ip.txt").openConnection() as java.net.HttpURLConnection
        conn1.connectTimeout = 4000; conn1.readTimeout = 4000
        try {
            return conn1.inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) {
        } finally { conn1.disconnect() }
        val conn2 = URL("https://checkip.amazonaws.com").openConnection() as java.net.HttpURLConnection
        conn2.connectTimeout = 4000; conn2.readTimeout = 4000
        return try {
            conn2.inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) { "Unavailable" } finally { conn2.disconnect() }
    }

    private fun fetchPublicIpv6(): String {
        val conn = URL("https://api6.my-ip.io/v2/ip.txt").openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 3000; conn.readTimeout = 3000
        return try {
            conn.inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) { "None" } finally { conn.disconnect() }
    }

    @Suppress("DEPRECATION")
    private fun getLocalInfo(): Pair<String, String> {
        val dhcp = wifiManager.dhcpInfo
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo?.ipAddress?.let { intToIp(it) } ?: "Unknown"
        val gw = dhcp?.gateway?.let { intToIp(it) } ?: "Unknown"
        return ip to gw
    }

    // ── Wake on LAN ───────────────────────────────────────────────────────────

    suspend fun wakeOnLan(macAddress: String, broadcastIp: String = "255.255.255.255"): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val macBytes = macAddress.split(":", "-").map { it.toInt(16).toByte() }.toByteArray()
                if (macBytes.size != 6) return@withContext false

                // Magic packet: 6x 0xFF + 16x MAC
                val magic = ByteArray(102)
                for (i in 0..5) magic[i] = 0xFF.toByte()
                for (i in 1..16) {
                    for (j in 0..5) magic[i * 6 + j] = macBytes[j]
                }

                val socket = DatagramSocket()
                socket.setBroadcast(true)
                val packet = DatagramPacket(magic, magic.size, InetAddress.getByName(broadcastIp), 9)
                socket.send(packet)
                socket.close()
                true
            } catch (e: Exception) { false }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun getDnsServer(): String {
        val dhcp = wifiManager.dhcpInfo
        return dhcp?.dns1?.let { intToIp(it) } ?: "Unknown"
    }

    private fun intToIp(ip: Int): String =
        "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"

    fun getServiceName(port: Int): String = when (port) {
        20 -> "FTP-Data"; 21 -> "FTP"; 22 -> "SSH"; 23 -> "Telnet"
        25 -> "SMTP"; 53 -> "DNS"; 67 -> "DHCP"; 80 -> "HTTP"
        110 -> "POP3"; 111 -> "RPC"; 119 -> "NNTP"; 123 -> "NTP"
        139 -> "NetBIOS"; 143 -> "IMAP"; 161 -> "SNMP"; 194 -> "IRC"
        443 -> "HTTPS"; 445 -> "SMB"; 465 -> "SMTPS"; 514 -> "Syslog"
        554 -> "RTSP"; 587 -> "SMTP-TLS"; 631 -> "IPP/CUPS"; 993 -> "IMAPS"
        995 -> "POP3S"; 1080 -> "SOCKS"; 1194 -> "OpenVPN"; 1433 -> "MSSQL"
        1723 -> "PPTP"; 3306 -> "MySQL"; 3389 -> "RDP"; 4444 -> "Metasploit"
        5432 -> "PostgreSQL"; 5900 -> "VNC"; 6379 -> "Redis"; 6881 -> "BitTorrent"
        8080 -> "HTTP-Alt"; 8443 -> "HTTPS-Alt"; 8888 -> "HTTP-Alt2"
        9200 -> "Elasticsearch"; 27017 -> "MongoDB"
        else -> "Port $port"
    }

    companion object {
        val TOP_PORTS = listOf(
            21, 22, 23, 25, 53, 80, 110, 111, 135, 139, 143, 161,
            443, 445, 465, 514, 554, 587, 631, 993, 995,
            1080, 1433, 1723, 3306, 3389, 4444, 5432, 5900,
            6379, 8080, 8443, 9200, 27017
        )
        val QUICK_PORTS = listOf(21, 22, 23, 80, 139, 443, 445, 3389, 5900, 8080)
    }
}
