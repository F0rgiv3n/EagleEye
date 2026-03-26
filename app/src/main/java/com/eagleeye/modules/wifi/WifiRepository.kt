package com.eagleeye.modules.wifi

import android.content.Context
import android.net.ConnectivityManager
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
        val wifiInfo = wifiManager.connectionInfo
        val dhcpInfo = wifiManager.dhcpInfo

        if (wifiInfo == null || wifiInfo.networkId == -1) {
            return WifiConnectionInfo(isConnected = false)
        }

        val rawSsid = wifiInfo.ssid ?: ""
        val ssid = rawSsid.removePrefix("\"").removeSuffix("\"")

        return WifiConnectionInfo(
            ssid = ssid,
            bssid = wifiInfo.bssid ?: "",
            ipAddress = intToIp(wifiInfo.ipAddress),
            gateway = intToIp(dhcpInfo.gateway),
            subnetMask = intToIp(dhcpInfo.netmask),
            dns1 = intToIp(dhcpInfo.dns1),
            dns2 = intToIp(dhcpInfo.dns2),
            linkSpeedMbps = wifiInfo.linkSpeed,
            rssi = wifiInfo.rssi,
            frequencyMhz = wifiInfo.frequency,
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
