package com.eagleeye.modules.mac

import android.content.Context
import android.net.wifi.WifiManager
import com.eagleeye.data.MacInfo
import com.eagleeye.data.MacProfile
import com.eagleeye.data.MacType
import com.eagleeye.data.db.AppDatabase
import com.eagleeye.modules.lan.OuiLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.NetworkInterface

class MacRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).macProfileDao()
    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    val profiles: Flow<List<MacProfile>> = dao.observeAll()

    // ── MAC Info ──────────────────────────────────────────────────────────────

    suspend fun getMacInfo(): MacInfo = withContext(Dispatchers.IO) {
        OuiLookup.load(context)

        val currentMac = getCurrentMac()
        val type = detectMacType(currentMac)
        val vendor = OuiLookup.lookup(currentMac)
        val hardwareMac = getHardwareMacRoot()

        MacInfo(
            currentMac = currentMac,
            type = type,
            vendor = vendor,
            isRandomized = type == MacType.RANDOMIZED,
            hardwareMac = hardwareMac
        )
    }

    private fun getCurrentMac(): String {
        // Primary: read from NetworkInterface (wlan0)
        return try {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.firstOrNull { it.name.startsWith("wlan") }
                ?.hardwareAddress
                ?.joinToString(":") { "%02X".format(it) }
                ?: readMacFromProc()
        } catch (e: Exception) {
            readMacFromProc()
        }
    }

    private fun readMacFromProc(): String {
        return try {
            java.io.File("/sys/class/net/wlan0/address")
                .readText().trim().uppercase()
        } catch (e: Exception) {
            try {
                java.io.File("/sys/class/net/wlan1/address")
                    .readText().trim().uppercase()
            } catch (e2: Exception) { "Unknown" }
        }
    }

    private fun getHardwareMacRoot(): String {
        // Attempt root read of persistent MAC (stored in nvram / persist partition)
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /sys/class/net/wlan0/address"))
            proc.inputStream.bufferedReader().readLine()?.trim()?.uppercase() ?: ""
        } catch (e: Exception) { "" }
    }

    /**
     * Detect if MAC is randomized:
     * - Android randomized MACs have the locally administered bit set (bit 1 of first octet)
     * - Real OUI MACs have this bit = 0
     */
    private fun detectMacType(mac: String): MacType {
        if (mac == "Unknown" || mac.isBlank()) return MacType.UNKNOWN
        val firstOctet = mac.split(":").firstOrNull()?.toIntOrNull(16) ?: return MacType.UNKNOWN
        val isLocallyAdministered = (firstOctet and 0x02) != 0
        return if (isLocallyAdministered) MacType.RANDOMIZED else MacType.REAL
    }

    // ── MAC Change (root) ─────────────────────────────────────────────────────

    suspend fun changeMac(newMac: String): MacChangeResult = withContext(Dispatchers.IO) {
        if (!isValidMac(newMac)) return@withContext MacChangeResult.InvalidFormat

        try {
            val commands = arrayOf(
                "ip link set wlan0 down",
                "ip link set wlan0 address $newMac",
                "ip link set wlan0 up"
            )
            val script = commands.joinToString(" && ")
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            val exitCode = proc.waitFor()

            if (exitCode == 0) MacChangeResult.Success
            else MacChangeResult.RootDenied
        } catch (e: SecurityException) {
            MacChangeResult.RootDenied
        } catch (e: Exception) {
            MacChangeResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun resetToRandom(): MacChangeResult = withContext(Dispatchers.IO) {
        // Generate a proper locally-administered random MAC
        val random = java.util.Random()
        val bytes = ByteArray(6) { random.nextInt(256).toByte() }
        bytes[0] = ((bytes[0].toInt() and 0xFE) or 0x02).toByte()  // locally administered, unicast
        val mac = bytes.joinToString(":") { "%02X".format(it) }
        changeMac(mac)
    }

    fun generateRandomMac(): String {
        val random = java.util.Random()
        val bytes = ByteArray(6) { random.nextInt(256).toByte() }
        bytes[0] = ((bytes[0].toInt() and 0xFE) or 0x02).toByte()
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    private fun isValidMac(mac: String): Boolean =
        mac.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))

    // ── Profiles ──────────────────────────────────────────────────────────────

    suspend fun saveProfile(profile: MacProfile) = dao.upsert(profile)
    suspend fun deleteProfile(profile: MacProfile) = dao.delete(profile)
    suspend fun getProfileForSsid(ssid: String) = dao.getForSsid(ssid)

    suspend fun applyProfileForCurrentNetwork(): MacChangeResult = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        val ssid = wifiManager.connectionInfo?.ssid
            ?.removePrefix("\"")?.removeSuffix("\"")
            ?: return@withContext MacChangeResult.Error("Not connected")

        val profile = dao.getForSsid(ssid)
            ?: return@withContext MacChangeResult.Error("No profile for $ssid")

        val result = changeMac(profile.mac)
        if (result == MacChangeResult.Success) {
            dao.updateLastRotated(ssid, System.currentTimeMillis())
        }
        result
    }
}

sealed class MacChangeResult {
    object Success : MacChangeResult()
    object RootDenied : MacChangeResult()
    object InvalidFormat : MacChangeResult()
    data class Error(val message: String) : MacChangeResult()
}
