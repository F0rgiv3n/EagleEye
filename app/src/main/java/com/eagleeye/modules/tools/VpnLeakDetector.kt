package com.eagleeye.modules.tools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.eagleeye.data.VpnLeakResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.net.*

class VpnLeakDetector(private val context: Context) {

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    suspend fun detect(): VpnLeakResult = withContext(Dispatchers.IO) {
        val isVpnActive = isVpnConnected()
        val leakDetails = mutableListOf<String>()

        // Fetch public IP and DNS simultaneously
        val ipDeferred = async { fetchPublicIp() }
        val dnsDeferred = async { getActiveDnsServers() }
        val ipv6Deferred = async { fetchPublicIpv6() }

        val publicIp = ipDeferred.await()
        val dnsServers = dnsDeferred.await()
        val ipv6 = ipv6Deferred.await()

        // DNS Leak: if VPN is active but DNS servers are local/ISP addresses
        var dnsLeak = false
        if (isVpnActive) {
            val localDns = dnsServers.filter { isPrivateIp(it) }
            if (localDns.isNotEmpty()) {
                dnsLeak = true
                leakDetails.add("DNS Leak: queries going to ${localDns.joinToString(", ")} (local network) instead of VPN DNS")
            }
            // Check if DNS resolves via known ISP resolvers
            val suspiciousDns = dnsServers.filter { !isPrivateIp(it) && !isVpnDns(it) }
            if (suspiciousDns.isNotEmpty()) {
                dnsLeak = true
                leakDetails.add("Potential DNS leak: unrecognized resolver ${suspiciousDns.joinToString(", ")}")
            }
        }

        // IPv6 Leak: public IPv6 address while on VPN (tunnel may not cover IPv6)
        val ipv6Leak = isVpnActive && ipv6.isNotBlank() && ipv6 != "None" && !ipv6.startsWith("fc") && !ipv6.startsWith("fd")
        if (ipv6Leak) {
            leakDetails.add("IPv6 Leak: public IPv6 $ipv6 is exposed while VPN is active")
        }

        // No leaks found but VPN active
        if (isVpnActive && leakDetails.isEmpty()) {
            leakDetails.add("No leaks detected — VPN appears to be routing all traffic correctly")
        }

        // Not on VPN
        if (!isVpnActive) {
            leakDetails.add("No VPN detected — your real IP ($publicIp) and DNS are visible to the network")
        }

        VpnLeakResult(
            isVpnActive = isVpnActive,
            publicIpWithVpn = publicIp,
            dnsServers = dnsServers,
            dnsLeakDetected = dnsLeak,
            ipv6LeakDetected = ipv6Leak,
            ipv6Address = ipv6,
            leakDetails = leakDetails
        )
    }

    private fun isVpnConnected(): Boolean {
        val networks = connectivityManager.allNetworks
        return networks.any { network ->
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return@any false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
    }

    @Suppress("DEPRECATION")
    private fun getActiveDnsServers(): List<String> {
        val servers = mutableListOf<String>()
        // Read from VPN/system DNS via dhcpInfo
        val dhcp = wifiManager.dhcpInfo
        dhcp?.dns1?.takeIf { it != 0 }?.let { servers.add(intToIp(it)) }
        dhcp?.dns2?.takeIf { it != 0 }?.let { servers.add(intToIp(it)) }

        // Also try reading from /etc/resolv.conf (may work on some devices)
        try {
            val resolv = java.io.File("/etc/resolv.conf")
            if (resolv.exists()) {
                resolv.bufferedReader().lineSequence()
                    .filter { it.startsWith("nameserver") }
                    .map { it.substringAfter("nameserver").trim() }
                    .filter { it.isNotBlank() }
                    .forEach { if (it !in servers) servers.add(it) }
            }
        } catch (_: Exception) {}

        return servers.distinct()
    }

    private fun fetchPublicIp(): String {
        val conn1 = URL("https://api4.my-ip.io/v2/ip.txt").openConnection() as java.net.HttpURLConnection
        conn1.connectTimeout = 5000; conn1.readTimeout = 5000
        try {
            return conn1.inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) {
        } finally { conn1.disconnect() }
        val conn2 = URL("https://checkip.amazonaws.com").openConnection() as java.net.HttpURLConnection
        conn2.connectTimeout = 5000; conn2.readTimeout = 5000
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

    private fun isPrivateIp(ip: String): Boolean {
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return parts[0] == 10 ||
               (parts[0] == 172 && parts[1] in 16..31) ||
               (parts[0] == 192 && parts[1] == 168)
    }

    private fun isVpnDns(ip: String): Boolean = ip in setOf(
        "8.8.8.8", "8.8.4.4", "1.1.1.1", "1.0.0.1",
        "9.9.9.9", "208.67.222.222", "208.67.220.220",
        "10.0.0.1", "10.8.0.1" // common VPN gateway IPs used as DNS
    )

    private fun intToIp(ip: Int) =
        "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
}
