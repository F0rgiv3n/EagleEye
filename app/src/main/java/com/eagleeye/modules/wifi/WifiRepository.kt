package com.eagleeye.modules.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.eagleeye.data.ScannedNetwork
import com.eagleeye.data.WifiConnectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetAddress

class WifiRepository(private val context: Context) {

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observeConnectionInfo(): Flow<WifiConnectionInfo> = flow {
        while (true) {
            emit(getConnectionInfo())
            delay(3000)
        }
    }.flowOn(Dispatchers.IO)

    @Suppress("DEPRECATION")
    fun getConnectionInfo(): WifiConnectionInfo {
        // Android 10+ restricts WifiManager.connectionInfo — use ConnectivityManager
        // to reliably detect whether Wi-Fi is active before reading WifiInfo details.
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isWifiActive = caps != null &&
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        // Fallback: also accept if wifiInfo reports a valid IP even without caps
        val wifiInfo = wifiManager.connectionInfo
        val dhcpInfo = wifiManager.dhcpInfo
        val hasIp = (wifiInfo?.ipAddress ?: 0) != 0

        if (!isWifiActive && !hasIp) {
            return WifiConnectionInfo(isConnected = false)
        }

        val rawSsid = wifiInfo?.ssid ?: ""
        val ssid = rawSsid.removePrefix("\"").removeSuffix("\"")
            .let { if (it == "<unknown ssid>") "" else it }

        return WifiConnectionInfo(
            ssid = ssid,
            bssid = wifiInfo?.bssid ?: "",
            ipAddress = intToIp(wifiInfo?.ipAddress ?: 0),
            gateway = intToIp(dhcpInfo?.gateway ?: 0),
            subnetMask = intToIp(dhcpInfo?.netmask ?: 0),
            dns1 = intToIp(dhcpInfo?.dns1 ?: 0),
            dns2 = intToIp(dhcpInfo?.dns2 ?: 0),
            linkSpeedMbps = wifiInfo?.linkSpeed ?: -1,
            rssi = wifiInfo?.rssi ?: -100,
            frequencyMhz = wifiInfo?.frequency ?: 0,
            securityType = getSecurityType(),
            isConnected = true
        )
    }

    @Suppress("DEPRECATION")
    fun getScanResults(): List<ScannedNetwork> {
        return wifiManager.scanResults?.map { result ->
            ScannedNetwork(
                ssid = if (result.SSID.isNullOrEmpty()) "<Hidden>" else result.SSID,
                bssid = result.BSSID ?: "",
                rssi = result.level,
                frequencyMhz = result.frequency,
                securityType = parseCapabilities(result.capabilities),
                isHidden = result.SSID.isNullOrEmpty()
            )
        }?.sortedByDescending { it.rssi } ?: emptyList()
    }

    @Suppress("DEPRECATION")
    private fun getSecurityType(): String {
        val scanResult = wifiManager.scanResults?.firstOrNull { result ->
            val currentBssid = wifiManager.connectionInfo?.bssid
            result.BSSID == currentBssid
        }
        return if (scanResult != null) parseCapabilities(scanResult.capabilities) else ""
    }

    private fun parseCapabilities(capabilities: String): String = when {
        capabilities.contains("WPA3") -> "WPA3"
        capabilities.contains("WPA2") -> "WPA2"
        capabilities.contains("WPA") -> "WPA"
        capabilities.contains("WEP") -> "WEP"
        else -> "OPEN"
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }
}
