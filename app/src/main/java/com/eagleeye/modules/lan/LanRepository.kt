package com.eagleeye.modules.lan

import android.content.Context
import android.net.wifi.WifiManager
import com.eagleeye.data.LanDevice
import com.eagleeye.data.db.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.*

class LanRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).lanDeviceDao()
    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    val savedDevices: Flow<List<LanDevice>> = dao.observeAll()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress = _scanProgress.asStateFlow()

    // Common ports to check: HTTP, HTTPS, SSH, FTP, Telnet, SMB, RDP, RTSP, DNS, mDNS
    private val topPorts = listOf(21, 22, 23, 25, 53, 80, 110, 139, 143, 443, 445, 554, 3389, 5900, 8080, 8443)

    suspend fun scanNetwork(): List<LanDevice> = withContext(Dispatchers.IO) {
        OuiLookup.load(context)

        val subnet = getSubnetPrefix() ?: return@withContext emptyList()
        val myIp = getLocalIp() ?: return@withContext emptyList()

        val hosts = (1..254).map { it }
        val total = hosts.size.toFloat()
        val found = mutableListOf<LanDevice>()
        val mutex = Mutex()

        hosts.chunked(20).forEachIndexed { chunkIdx, chunk ->
            val jobs = chunk.map { lastOctet ->
                async {
                    val ip = "$subnet.$lastOctet"
                    if (ip == myIp) return@async

                    val latency = ping(ip)
                    if (latency >= 0) {
                        val mac = getMacFromArp(ip)
                        val hostname = resolveHostname(ip)
                        val vendor = OuiLookup.lookup(mac)
                        val ports = scanPorts(ip)
                        val now = System.currentTimeMillis()

                        val existing = dao.getByMac(mac.ifBlank { ip })
                        val device = LanDevice(
                            mac = mac.ifBlank { ip },
                            ip = ip,
                            hostname = hostname,
                            vendor = vendor,
                            isOnline = true,
                            latencyMs = latency,
                            openPorts = ports.joinToString(","),
                            firstSeen = existing?.firstSeen ?: now,
                            lastSeen = now,
                            isKnown = existing?.isKnown ?: false,
                            alias = existing?.alias ?: ""
                        )
                        dao.upsert(device)
                        mutex.withLock { found.add(device) }
                    }
                    mutex.withLock {
                        _scanProgress.value = (chunkIdx * 20f + chunk.indexOf(lastOctet) + 1) / total
                    }
                }
            }
            jobs.awaitAll()
        }

        _scanProgress.value = 1f
        found.sortedBy { ipToLong(it.ip) }
    }

    private fun ping(ip: String): Long {
        return try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(ip).isReachable(300)
            if (reachable) System.currentTimeMillis() - start else -1L
        } catch (e: Exception) {
            -1L
        }
    }

    private fun getMacFromArp(ip: String): String {
        return try {
            val arp = java.io.File("/proc/net/arp")
            if (!arp.exists()) return ""
            arp.bufferedReader().useLines { lines ->
                lines.drop(1).firstOrNull { it.startsWith(ip + " ") || it.contains(" $ip ") }
                    ?.split("\\s+".toRegex())
                    ?.getOrNull(3)
                    ?.uppercase()
                    ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun resolveHostname(ip: String): String {
        return try {
            withTimeoutOrNull(500) {
                withContext(Dispatchers.IO) {
                    InetAddress.getByName(ip).hostName.takeIf { it != ip } ?: ""
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun scanPorts(ip: String): List<Int> = coroutineScope {
        topPorts.map { port ->
            async(Dispatchers.IO) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), 200)
                        port
                    }
                } catch (_: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }

    fun getServiceName(port: Int): String = when (port) {
        21 -> "FTP"; 22 -> "SSH"; 23 -> "Telnet"; 25 -> "SMTP"
        53 -> "DNS"; 80 -> "HTTP"; 110 -> "POP3"; 139 -> "NetBIOS"
        143 -> "IMAP"; 443 -> "HTTPS"; 445 -> "SMB"; 554 -> "RTSP"
        3389 -> "RDP"; 5900 -> "VNC"; 8080 -> "HTTP-Alt"; 8443 -> "HTTPS-Alt"
        else -> "Port $port"
    }

    suspend fun markDeviceKnown(mac: String, known: Boolean) = dao.setKnown(mac, known)
    suspend fun setDeviceAlias(mac: String, alias: String) = dao.setAlias(mac, alias)

    private fun getLocalIp(): String? {
        // Primary: NetworkInterface — works on all API levels without deprecated APIs
        try {
            NetworkInterface.getNetworkInterfaces()?.asSequence()?.forEach { iface ->
                if (!iface.isUp || iface.isLoopback) return@forEach
                iface.inetAddresses.asSequence()
                    .filterIsInstance<Inet4Address>()
                    .filter { !it.isLoopbackAddress }
                    .firstOrNull()?.let { return it.hostAddress }
            }
        } catch (_: Exception) {}

        // Fallback: WifiManager (deprecated API 31 but still functional on most devices)
        @Suppress("DEPRECATION")
        val ip = wifiManager.connectionInfo?.ipAddress ?: 0
        if (ip == 0) return null
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }

    @Suppress("DEPRECATION")
    private fun getSubnetPrefix(): String? {
        val dhcp = wifiManager.dhcpInfo ?: return null
        val gw = dhcp.gateway
        if (gw == 0) return null
        val a = gw and 0xFF
        val b = (gw shr 8) and 0xFF
        val c = (gw shr 16) and 0xFF
        return "$a.$b.$c"
    }

    private fun ipToLong(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) return 0L
        return parts.fold(0L) { acc, s -> (acc shl 8) + (s.toLongOrNull() ?: 0L) }
    }

}
