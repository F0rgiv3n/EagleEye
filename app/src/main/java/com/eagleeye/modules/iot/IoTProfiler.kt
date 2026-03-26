package com.eagleeye.modules.iot

import com.eagleeye.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fingerprints LAN devices to determine category, model, and security risks.
 * Uses: OUI prefix, open ports, HTTP banner, SSDP services, hostname patterns.
 */
class IoTProfiler {

    suspend fun profile(device: LanDevice, ssdpDevices: List<SsdpDevice>): IoTProfile =
        withContext(Dispatchers.IO) {
            val ports     = device.openPortList
            val vendor    = device.vendor.lowercase()
            val hostname  = device.hostname.lowercase()
            val ssdp      = ssdpDevices.filter { it.ip == device.ip }

            // Fetch HTTP banner from admin panel (port 80 or 8080)
            val adminPort = ports.firstOrNull { it == 80 || it == 8080 || it == 443 || it == 8443 } ?: 0
            val httpBanner = if (adminPort > 0) fetchHttpBanner(device.ip, adminPort) else ""

            // Determine category
            val category = detectCategory(vendor, hostname, ports, httpBanner, ssdp)

            // Model guess from banner / vendor
            val model = guessModel(vendor, httpBanner, ssdp)

            // Default credentials
            val creds = defaultCredentials(vendor, category, httpBanner)

            // Risk assessment
            val risk = assessRisk(category, ports, creds, device.isKnown)

            IoTProfile(
                ip                    = device.ip,
                mac                   = device.mac,
                category              = category,
                deviceModel           = model,
                firmwareHint          = extractFirmware(httpBanner),
                hasAdminPanel         = adminPort > 0,
                adminPort             = adminPort,
                hasDefaultCredentials = creds.isNotEmpty(),
                defaultCreds          = creds,
                services              = ssdp.map { it.server.take(40) }.distinct(),
                riskLevel             = risk
            )
        }

    // ── Category Detection ────────────────────────────────────────────────────

    private fun detectCategory(
        vendor: String, hostname: String,
        ports: List<Int>, banner: String, ssdp: List<SsdpDevice>
    ): DeviceCategory {
        val all = "$vendor $hostname $banner ${ssdp.joinToString { it.server + it.st }}"
            .lowercase()

        return when {
            matchesAny(all, "camera", "cam", "dvr", "nvr", "hikvision", "dahua",
                "axis", "foscam", "reolink", "amcrest", "vivotek") ||
                ports.contains(554) -> DeviceCategory.CAMERA

            matchesAny(all, "router", "gateway", "fritz", "asus", "tp-link", "tplink",
                "netgear", "linksys", "ubiquiti", "mikrotik", "dlink", "d-link",
                "openwrt", "dd-wrt", "tomato", "cisco") ||
                (ports.contains(80) && ports.contains(53)) -> DeviceCategory.ROUTER

            matchesAny(all, "samsung", "lg", "sony", "bravia", "tizen", "webos",
                "smart tv", "smarttv", "vizio", "hisense", "philips tv") ||
                ports.contains(8001) || ports.contains(8002) -> DeviceCategory.SMART_TV

            matchesAny(all, "printer", "print", "canon", "epson", "hp", "brother",
                "xerox", "kyocera", "lexmark", "ricoh", "jetdirect") ||
                ports.contains(9100) || ports.contains(515) -> DeviceCategory.PRINTER

            matchesAny(all, "synology", "qnap", "western digital", "wd", "seagate",
                "nas", "diskstation", "netshare") -> DeviceCategory.NAS

            matchesAny(all, "playstation", "xbox", "nintendo", "sony computer") ||
                ports.contains(3659) -> DeviceCategory.GAME_CONSOLE

            matchesAny(all, "echo", "alexa", "google home", "nest", "homepod",
                "sonos", "speaker") -> DeviceCategory.SMART_SPEAKER

            matchesAny(all, "android", "iphone", "ipad", "apple", "samsung mobile",
                "oneplus", "pixel") -> DeviceCategory.PHONE

            matchesAny(all, "windows", "macos", "ubuntu", "debian", "linux pc",
                "workstation") ||
                ports.contains(3389) || ports.contains(5900) -> DeviceCategory.PC

            matchesAny(all, "shelly", "tasmota", "tuya", "zigbee", "zwave",
                "esp8266", "esp32", "arduino", "raspberry", "sense", "sensor",
                "thermostat", "bulb", "switch", "plug") -> DeviceCategory.IOT_SENSOR

            else -> DeviceCategory.UNKNOWN
        }
    }

    private fun matchesAny(text: String, vararg keywords: String) =
        keywords.any { text.contains(it) }

    // ── Model Guess ───────────────────────────────────────────────────────────

    private fun guessModel(vendor: String, banner: String, ssdp: List<SsdpDevice>): String {
        // Try SSDP server field first (most accurate)
        ssdp.firstOrNull { it.server.isNotBlank() }?.server?.let {
            val clean = it.split("/").firstOrNull()?.trim()
            if (!clean.isNullOrBlank()) return clean
        }

        // Try HTTP Server header
        val serverHeader = Regex("Server:\\s*([^\r\n]+)", RegexOption.IGNORE_CASE)
            .find(banner)?.groupValues?.get(1)?.trim()
        if (!serverHeader.isNullOrBlank()) return serverHeader.take(50)

        // Fallback to vendor
        return vendor.replaceFirstChar { it.uppercase() }.take(30)
    }

    // ── HTTP Banner ───────────────────────────────────────────────────────────

    private fun fetchHttpBanner(ip: String, port: Int): String {
        return try {
            val scheme = if (port == 443 || port == 8443) "https" else "http"
            val conn = URL("$scheme://$ip:$port/").openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout    = 2000
            conn.instanceFollowRedirects = false
            conn.requestMethod = "HEAD"
            val headers = buildString {
                conn.headerFields.entries.take(20).forEach { (k, v) ->
                    if (k != null) appendLine("$k: ${v.joinToString("; ")}")
                }
            }
            // Also try GET for title
            val body = try {
                conn.requestMethod = "GET"
                conn.inputStream.bufferedReader().use { it.readText().take(300) }
            } catch (_: Exception) { "" }
            "$headers\n$body"
        } catch (_: Exception) { "" }
    }

    private fun extractFirmware(banner: String): String {
        val patterns = listOf(
            Regex("firmware[/ :]([\\d.]+)", RegexOption.IGNORE_CASE),
            Regex("version[/ :]([\\d.]+)", RegexOption.IGNORE_CASE),
            Regex("v([\\d]+\\.[\\d]+\\.[\\d]+)")
        )
        return patterns.firstNotNullOfOrNull {
            it.find(banner)?.groupValues?.getOrNull(1)
        } ?: ""
    }

    // ── Default Credentials ───────────────────────────────────────────────────

    private fun defaultCredentials(
        vendor: String, category: DeviceCategory, banner: String
    ): List<Pair<String, String>> {
        val v = vendor.lowercase()
        val b = banner.lowercase()

        return buildList {
            // Router defaults
            if (category == DeviceCategory.ROUTER) {
                if (matchesAny(v, "tp-link", "tplink")) addAll(listOf("admin" to "admin", "admin" to ""))
                if (matchesAny(v, "netgear"))            addAll(listOf("admin" to "password", "admin" to "1234"))
                if (matchesAny(v, "asus"))               addAll(listOf("admin" to "admin"))
                if (matchesAny(v, "dlink", "d-link"))    addAll(listOf("admin" to "", "admin" to "admin"))
                if (matchesAny(v, "linksys"))            addAll(listOf("admin" to "admin", "" to "admin"))
                if (matchesAny(v, "fritz"))              addAll(listOf("" to ""))
                if (matchesAny(v, "ubiquiti"))           addAll(listOf("ubnt" to "ubnt"))
                if (matchesAny(v, "mikrotik"))           addAll(listOf("admin" to ""))
                if (isEmpty()) addAll(listOf("admin" to "admin", "admin" to "1234", "admin" to ""))
            }
            // Camera defaults
            if (category == DeviceCategory.CAMERA) {
                if (matchesAny(v, b, "hikvision"))   addAll(listOf("admin" to "12345", "admin" to ""))
                if (matchesAny(v, b, "dahua"))       addAll(listOf("admin" to "admin", "admin" to ""))
                if (matchesAny(v, b, "axis"))        addAll(listOf("root" to "pass", "admin" to "admin"))
                if (matchesAny(v, b, "foscam"))      addAll(listOf("admin" to "", "admin" to "admin"))
                if (isEmpty()) addAll(listOf("admin" to "admin", "admin" to "12345", "root" to "root"))
            }
            // NAS defaults
            if (category == DeviceCategory.NAS) {
                if (matchesAny(v, b, "synology")) addAll(listOf("admin" to "admin", "admin" to ""))
                if (matchesAny(v, b, "qnap"))     addAll(listOf("admin" to "admin", "admin" to ""))
                if (isEmpty()) addAll(listOf("admin" to "admin"))
            }
            // Printer defaults
            if (category == DeviceCategory.PRINTER) {
                if (matchesAny(v, "hp"))     addAll(listOf("admin" to "", "" to ""))
                if (matchesAny(v, "canon"))  addAll(listOf("admin" to "canon", "ADMIN" to "canon"))
                if (matchesAny(v, "epson"))  addAll(listOf("" to "admin"))
                if (isEmpty()) addAll(listOf("admin" to "admin", "admin" to ""))
            }
        }.distinct()
    }

    // ── Risk Assessment ───────────────────────────────────────────────────────

    private fun assessRisk(
        category: DeviceCategory,
        ports: List<Int>,
        creds: List<Pair<String, String>>,
        isKnown: Boolean
    ): IoTRisk {
        var score = 0
        if (creds.isNotEmpty())              score += 3
        if (!isKnown)                        score += 2
        if (ports.contains(23))              score += 3  // Telnet
        if (ports.contains(21))              score += 2  // FTP
        if (category == DeviceCategory.CAMERA) score += 1
        if (category == DeviceCategory.ROUTER) score += 1

        return when {
            score >= 5 -> IoTRisk.HIGH
            score >= 3 -> IoTRisk.MEDIUM
            score >= 1 -> IoTRisk.LOW
            else       -> IoTRisk.SAFE
        }
    }
}
