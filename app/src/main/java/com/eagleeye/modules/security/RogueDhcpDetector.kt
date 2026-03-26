package com.eagleeye.modules.security

import android.content.Context
import android.net.wifi.WifiManager
import com.eagleeye.data.RogueDhcpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class RogueDhcpDetector(private val context: Context) {

    @Suppress("DEPRECATION")
    suspend fun detect(): RogueDhcpResult = withContext(Dispatchers.IO) {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wm.dhcpInfo

        val knownGateway = intToIp(dhcp.gateway)
        val knownServer  = intToIp(dhcp.serverAddress)
        val myIp         = intToIp(dhcp.ipAddress)
        val subnet       = myIp.substringBeforeLast(".")

        // Scan the subnet for devices that have TCP port 80 AND DNS (53) open
        // — this profile matches routers/DHCP servers
        val candidates = (1..254)
            .map { it }
            .filter { last ->
                val ip = "$subnet.$last"
                ip != myIp && ip != knownGateway && ip != knownServer
            }
            .let { list ->
                // parallel check: respond to ping + have port 80 or 53
                list.map { last ->
                    async {
                        val ip = "$subnet.$last"
                        if (isReachable(ip) && (hasPort(ip, 80) || hasPort(ip, 53)))
                            ip else null
                    }
                }.awaitAll().filterNotNull()
            }

        val suspiciousIps = candidates.filter { ip ->
            // A device with DNS (53) + HTTP (80/443) that isn't the known gateway
            hasPort(ip, 53) && (hasPort(ip, 80) || hasPort(ip, 443))
        }

        // Check: gateway ≠ DHCP server (suspicious on flat /24 home nets)
        val serverGatewayMismatch = knownGateway != knownServer &&
            knownGateway != "0.0.0.0" && knownServer != "0.0.0.0" &&
            knownGateway.substringBeforeLast(".") == knownServer.substringBeforeLast(".")

        // Hostname check for suspicious gateway
        val gatewayHostname = runCatching {
            InetAddress.getByName(knownGateway).canonicalHostName
                .takeIf { it != knownGateway } ?: ""
        }.getOrDefault("")

        val findings = mutableListOf<String>()
        if (serverGatewayMismatch)
            findings.add("DHCP server ($knownServer) differs from gateway ($knownGateway) — possible MITM proxy")
        suspiciousIps.forEach { ip ->
            findings.add("Potential rogue DHCP/router at $ip (responds on DNS+HTTP, not the known gateway)")
        }

        RogueDhcpResult(
            knownGateway       = knownGateway,
            dhcpServer         = knownServer,
            gatewayHostname    = gatewayHostname,
            suspiciousServers  = suspiciousIps,
            serverGatewayMismatch = serverGatewayMismatch,
            findings           = findings,
            isClean            = findings.isEmpty()
        )
    }

    private fun isReachable(ip: String): Boolean = runCatching {
        InetAddress.getByName(ip).isReachable(300)
    }.getOrDefault(false)

    private fun hasPort(ip: String, port: Int): Boolean = runCatching {
        Socket().use { s ->
            s.connect(InetSocketAddress(ip, port), 300)
            true
        }
    }.getOrDefault(false)

    private fun intToIp(i: Int) =
        "${i and 0xFF}.${(i shr 8) and 0xFF}.${(i shr 16) and 0xFF}.${(i shr 24) and 0xFF}"
}
