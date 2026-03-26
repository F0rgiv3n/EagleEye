package com.eagleeye.modules.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.eagleeye.data.LanDevice
import com.eagleeye.data.NetworkEvent
import com.eagleeye.data.SecurityScore
import com.eagleeye.data.WifiConnectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReportExporter(private val context: Context) {

    suspend fun exportJson(
        wifi: WifiConnectionInfo,
        score: SecurityScore?,
        devices: List<LanDevice>,
        events: List<NetworkEvent> = emptyList()
    ): Intent = withContext(Dispatchers.IO) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(context.cacheDir, "eagleeye_report_$ts.json")
        file.writeText(buildJson(wifi, score, devices, events))
        buildShareIntent(file, "application/json")
    }

    suspend fun exportText(
        wifi: WifiConnectionInfo,
        score: SecurityScore?,
        devices: List<LanDevice>,
        events: List<NetworkEvent> = emptyList()
    ): Intent = withContext(Dispatchers.IO) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(context.cacheDir, "eagleeye_report_$ts.txt")
        file.writeText(buildText(wifi, score, devices, events))
        buildShareIntent(file, "text/plain")
    }

    private fun buildShareIntent(file: File, mimeType: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "EagleEye Security Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    private fun buildJson(
        wifi: WifiConnectionInfo,
        score: SecurityScore?,
        devices: List<LanDevice>,
        events: List<NetworkEvent> = emptyList()
    ): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val now = sdf.format(Date())
        val device = android.os.Build.MODEL

        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"report\": {")
        sb.appendLine("    \"generated\": \"$now\",")
        sb.appendLine("    \"device\": \"$device\",")
        sb.appendLine("    \"app\": \"EagleEye v1.0\"")
        sb.appendLine("  },")

        // Wi-Fi
        sb.appendLine("  \"wifi\": {")
        sb.appendLine("    \"ssid\": \"${wifi.ssid}\",")
        sb.appendLine("    \"bssid\": \"${wifi.bssid}\",")
        sb.appendLine("    \"ip\": \"${wifi.ipAddress}\",")
        sb.appendLine("    \"gateway\": \"${wifi.gateway}\",")
        sb.appendLine("    \"dns1\": \"${wifi.dns1}\",")
        sb.appendLine("    \"dns2\": \"${wifi.dns2}\",")
        sb.appendLine("    \"security\": \"${wifi.securityType}\",")
        sb.appendLine("    \"band\": \"${wifi.band}\",")
        sb.appendLine("    \"rssi\": ${wifi.rssi},")
        sb.appendLine("    \"linkSpeed\": ${wifi.linkSpeedMbps}")
        sb.appendLine("  },")

        // Security score
        if (score != null) {
            sb.appendLine("  \"security\": {")
            sb.appendLine("    \"score\": ${score.total},")
            sb.appendLine("    \"grade\": \"${score.grade}\",")
            sb.appendLine("    \"threats\": [")
            score.threats.forEachIndexed { i, t ->
                val comma = if (i < score.threats.size - 1) "," else ""
                sb.appendLine("      { \"id\": \"${t.id}\", \"level\": \"${t.level}\", \"title\": \"${t.title.replace("\"", "'")}\" }$comma")
            }
            sb.appendLine("    ]")
            sb.appendLine("  },")
        }

        // LAN devices
        sb.appendLine("  \"lan_devices\": [")
        devices.forEachIndexed { i, d ->
            val comma = if (i < devices.size - 1) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"ip\": \"${d.ip}\",")
            sb.appendLine("      \"mac\": \"${d.mac}\",")
            sb.appendLine("      \"hostname\": \"${d.hostname}\",")
            sb.appendLine("      \"vendor\": \"${d.vendor}\",")
            sb.appendLine("      \"open_ports\": [${d.openPortList.joinToString(", ")}],")
            sb.appendLine("      \"latency_ms\": ${d.latencyMs},")
            sb.appendLine("      \"known\": ${d.isKnown}")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ],")

        // Network history
        sb.appendLine("  \"network_events\": [")
        val evSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).also {
            it.timeZone = TimeZone.getTimeZone("UTC")
        }
        events.take(100).forEachIndexed { i, ev ->
            val comma = if (i < events.take(100).size - 1) "," else ""
            sb.appendLine("    { \"time\": \"${evSdf.format(Date(ev.timestamp))}\", \"type\": \"${ev.type}\", \"severity\": \"${ev.severity}\", \"title\": \"${ev.title.replace("\"", "'")}\" }$comma")
        }
        sb.appendLine("  ]")
        sb.append("}")

        return sb.toString()
    }

    // ── Plain Text ────────────────────────────────────────────────────────────

    private fun buildText(
        wifi: WifiConnectionInfo,
        score: SecurityScore?,
        devices: List<LanDevice>,
        events: List<NetworkEvent> = emptyList()
    ): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()

        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("         EAGLEEYE SECURITY REPORT      ")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("Generated : ${sdf.format(Date())}")
        sb.appendLine("Device    : ${android.os.Build.MODEL}")
        sb.appendLine()

        sb.appendLine("── NETWORK ─────────────────────────────")
        sb.appendLine("SSID       : ${wifi.ssid.ifBlank { "Not connected" }}")
        if (wifi.isConnected) {
            sb.appendLine("BSSID      : ${wifi.bssid}")
            sb.appendLine("IP         : ${wifi.ipAddress}")
            sb.appendLine("Gateway    : ${wifi.gateway}")
            sb.appendLine("DNS        : ${wifi.dns1}  /  ${wifi.dns2}")
            sb.appendLine("Security   : ${wifi.securityType}")
            sb.appendLine("Band       : ${wifi.band}")
            sb.appendLine("RSSI       : ${wifi.rssi} dBm  (${wifi.signalStrength.label})")
            sb.appendLine("Link Speed : ${wifi.linkSpeedMbps} Mbps")
        }
        sb.appendLine()

        if (score != null) {
            sb.appendLine("── SECURITY SCORE ──────────────────────")
            sb.appendLine("Score : ${score.total}/100  (Grade: ${score.grade})")
            sb.appendLine()
            if (score.threats.isEmpty()) {
                sb.appendLine("  ✓ No threats detected")
            } else {
                score.threats.forEach { t ->
                    val marker = when (t.level.name) {
                        "CRITICAL" -> "!!"; "HIGH" -> "! "; "MEDIUM" -> "? "
                        else -> "  "
                    }
                    sb.appendLine("  $marker [${t.level}] ${t.title}")
                    sb.appendLine("     → ${t.recommendation}")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("── LAN DEVICES (${devices.size}) ─────────────────")
        if (devices.isEmpty()) {
            sb.appendLine("  No scan data. Run LAN scan first.")
        } else {
            devices.forEach { d ->
                sb.appendLine("  ${d.ip.padEnd(16)} ${d.mac.padEnd(18)} ${d.vendor.take(20).padEnd(22)} ${if (d.isOnline) "${d.latencyMs}ms" else "offline"}")
                if (d.openPortList.isNotEmpty()) {
                    sb.appendLine("    Ports: ${d.openPortList.joinToString(", ")}")
                }
            }
        }
        sb.appendLine()
        if (events.isNotEmpty()) {
            val evSdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            sb.appendLine("── NETWORK HISTORY (last ${events.take(30).size}) ───────────")
            events.take(30).forEach { ev ->
                sb.appendLine("  [${ev.severity.name.padEnd(8)}] ${evSdf.format(Date(ev.timestamp))}  ${ev.title}")
            }
            sb.appendLine()
        }

        sb.appendLine("═══════════════════════════════════════")

        return sb.toString()
    }
}
