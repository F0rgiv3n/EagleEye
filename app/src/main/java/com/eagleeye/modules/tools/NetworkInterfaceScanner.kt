package com.eagleeye.modules.tools

import com.eagleeye.data.ArpCacheEntry
import com.eagleeye.data.IPv6AddressInfo
import com.eagleeye.data.IPv6InspectorResult
import com.eagleeye.data.NetworkInterfaceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class NetworkInterfaceScanner {

    suspend fun getInterfaces(): List<NetworkInterfaceInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NetworkInterfaceInfo>()
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return@withContext result
            for (iface in ifaces) {
                val ipv4 = iface.inetAddresses.toList()
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull()?.hostAddress ?: ""
                val ipv6 = iface.inetAddresses.toList()
                    .filterIsInstance<Inet6Address>()
                    .firstOrNull { !it.isLinkLocalAddress }
                    ?.hostAddress?.substringBefore('%') ?: ""
                val mac = iface.hardwareAddress
                    ?.joinToString(":") { "%02X".format(it) } ?: ""

                val type = when {
                    iface.isLoopback -> "Loopback"
                    iface.name.startsWith("wlan") || iface.name.startsWith("wifi") -> "Wi-Fi"
                    iface.name.startsWith("rmnet") || iface.name.startsWith("ccmni") ||
                    iface.name.startsWith("mbim")  -> "Cellular"
                    iface.name.startsWith("tun")   ||
                    iface.name.startsWith("ppp")   -> "VPN"
                    iface.name.startsWith("eth")   -> "Ethernet"
                    iface.name.startsWith("p2p")   -> "Wi-Fi Direct"
                    iface.name.startsWith("dummy") -> "Dummy"
                    else -> "Other"
                }
                if (type == "Dummy") continue

                val friendlyName = when (type) {
                    "Wi-Fi"        -> "Wi-Fi (${iface.name})"
                    "Cellular"     -> "Cellular (${iface.name})"
                    "VPN"          -> "VPN Tunnel (${iface.name})"
                    "Loopback"     -> "Loopback"
                    "Ethernet"     -> "Ethernet (${iface.name})"
                    "Wi-Fi Direct" -> "Wi-Fi Direct (${iface.name})"
                    else           -> iface.name
                }

                result += NetworkInterfaceInfo(
                    name         = iface.name,
                    friendlyName = friendlyName,
                    type         = type,
                    ipv4         = ipv4,
                    ipv6         = ipv6,
                    mac          = mac,
                    mtu          = runCatching { iface.mtu }.getOrDefault(0),
                    isUp         = iface.isUp,
                    isLoopback   = iface.isLoopback
                )
            }
        } catch (_: Exception) {}
        result.sortedWith(compareBy({ !it.isUp }, { it.isLoopback }, { it.name }))
    }

    suspend fun getArpCache(): List<ArpCacheEntry> = withContext(Dispatchers.IO) {
        try {
            File("/proc/net/arp").readLines().drop(1).mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 6) return@mapNotNull null
                val mac = parts[3]
                if (mac == "00:00:00:00:00:00") return@mapNotNull null
                ArpCacheEntry(
                    ip         = parts[0],
                    mac        = mac.uppercase(),
                    flags      = parts[2],
                    iface      = parts[5],
                    isComplete = parts[2] == "0x2"
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun inspectIPv6(): IPv6InspectorResult = withContext(Dispatchers.IO) {
        try {
            val addresses = mutableListOf<IPv6AddressInfo>()
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()

            for (iface in ifaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses.toList()) {
                    if (addr !is Inet6Address) continue
                    val hostAddr = addr.hostAddress?.substringBefore('%') ?: continue
                    val type = when {
                        addr.isLoopbackAddress  -> "Loopback"
                        addr.isLinkLocalAddress -> "Link-local"
                        hostAddr.startsWith("fc") || hostAddr.startsWith("fd") -> "Unique-local (ULA)"
                        hostAddr.startsWith("2") || hostAddr.startsWith("3")  -> "Global"
                        else -> "Other"
                    }
                    if (type == "Loopback") continue
                    addresses += IPv6AddressInfo(hostAddr, type, iface.name)
                }
            }

            val global    = addresses.firstOrNull { it.type == "Global" }
            val linkLocal = addresses.firstOrNull { it.type == "Link-local" }

            val hasConnectivity = if (global != null) {
                try {
                    Socket().use { s ->
                        s.connect(InetSocketAddress("ipv6.google.com", 80), 3000)
                        true
                    }
                } catch (_: Exception) { false }
            } else false

            val note = when {
                global != null && hasConnectivity  -> "Full IPv6 — global address + connectivity"
                global != null && !hasConnectivity -> "Global address present but no IPv6 connectivity"
                linkLocal != null                  -> "Link-local only — no global IPv6"
                else                               -> "No IPv6 support detected"
            }

            IPv6InspectorResult(
                addresses        = addresses,
                hasGlobalAddress = global != null,
                hasConnectivity  = hasConnectivity,
                globalAddress    = global?.address ?: "",
                linkLocalAddress = linkLocal?.address ?: "",
                note             = note
            )
        } catch (e: Exception) {
            IPv6InspectorResult(error = e.message ?: "Inspection failed")
        }
    }
}
