package com.eagleeye.modules.tools

import com.eagleeye.data.ThreatIntelResult
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ThreatIntelClient {

    private val abuseCategories = mapOf(
        3 to "Fraud Orders", 4 to "DDoS Attack", 5 to "FTP Brute-Force",
        6 to "Ping of Death", 7 to "Phishing", 9 to "Open Proxy",
        10 to "Web Spam", 11 to "Email Spam", 14 to "Port Scan",
        18 to "Brute-Force", 19 to "Bad Web Bot", 21 to "SQL Injection",
        22 to "Spoofing"
    )

    suspend fun check(ip: String, abuseIpDbKey: String = ""): ThreatIntelResult {
        if (!ip.first().isDigit() || !ip.contains('.')) {
            return ThreatIntelResult(ip = ip, error = "Invalid IP address format")
        }

        return try {
            val ipApiResult = fetchIpApi(ip)

            if (abuseIpDbKey.isBlank()) {
                ipApiResult
            } else {
                try {
                    enrichWithAbuse(ipApiResult, abuseIpDbKey)
                } catch (e: Exception) {
                    ipApiResult
                }
            }
        } catch (e: Exception) {
            ThreatIntelResult(ip = ip, error = e.message ?: "Request failed")
        }
    }

    private fun fetchIpApi(ip: String): ThreatIntelResult {
        val url = URL("http://ip-api.com/json/$ip?fields=66846719")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"

        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val json = JSONObject(body)
        val isProxy = json.optBoolean("proxy", false)
        val isHosting = json.optBoolean("hosting", false)

        val riskReasons = mutableListOf<String>()
        if (isProxy) riskReasons += "Detected as proxy/VPN"
        if (isHosting) riskReasons += "Datacenter/hosting IP"

        val riskLevel = when {
            isProxy && isHosting -> "HIGH RISK"
            isProxy || isHosting -> "SUSPICIOUS"
            else -> "SAFE"
        }

        return ThreatIntelResult(
            ip = json.optString("query", ip),
            country = json.optString("country", ""),
            countryCode = json.optString("countryCode", ""),
            city = json.optString("city", ""),
            isp = json.optString("isp", ""),
            org = json.optString("org", ""),
            asn = json.optString("as", ""),
            isProxy = isProxy,
            isHosting = isHosting,
            riskLevel = riskLevel,
            riskReasons = riskReasons
        )
    }

    private fun enrichWithAbuse(base: ThreatIntelResult, apiKey: String): ThreatIntelResult {
        val url = URL("https://api.abuseipdb.com/api/v2/check?ipAddress=${base.ip}&maxAgeInDays=90")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Key", apiKey)
        conn.setRequestProperty("Accept", "application/json")

        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val data = JSONObject(body).optJSONObject("data") ?: return base
        val abuseScore = data.optInt("abuseConfidenceScore", 0)
        val totalReports = data.optInt("totalReports", 0)

        val cats = mutableListOf<String>()
        data.optJSONArray("reports")?.let { arr ->
            for (i in 0 until arr.length()) {
                val report = arr.optJSONObject(i) ?: continue
                val catArr = report.optJSONArray("categories") ?: continue
                for (j in 0 until catArr.length()) {
                    val catId = catArr.optInt(j, -1)
                    abuseCategories[catId]?.let { if (!cats.contains(it)) cats += it }
                }
            }
        }

        val updatedReasons = base.riskReasons.toMutableList()
        if (abuseScore >= 50) updatedReasons += "AbuseIPDB score: $abuseScore%"
        val updatedRisk = if (abuseScore >= 50) "HIGH RISK" else base.riskLevel

        return base.copy(
            abuseScore = abuseScore,
            abuseReports = totalReports,
            abuseCategories = cats,
            riskLevel = updatedRisk,
            riskReasons = updatedReasons
        )
    }
}
