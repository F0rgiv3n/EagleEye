package com.eagleeye.modules.tools

import com.eagleeye.data.ShodanResult
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ShodanClient {

    suspend fun lookup(ip: String): ShodanResult {
        if (ip.isEmpty() || !ip.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))) {
            return ShodanResult(ip = ip, error = "Invalid IP address format")
        }

        return try {
            val url = URL("https://internetdb.shodan.io/$ip")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"

            try {
                val responseCode = conn.responseCode
                if (responseCode == 404) {
                    return ShodanResult(ip = ip)
                }

                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)

                val hostnames = mutableListOf<String>()
                json.optJSONArray("hostnames")?.let { arr ->
                    for (i in 0 until arr.length()) hostnames += arr.optString(i)
                }

                val ports = mutableListOf<Int>()
                json.optJSONArray("ports")?.let { arr ->
                    for (i in 0 until arr.length()) ports += arr.optInt(i)
                }

                val cves = mutableListOf<String>()
                json.optJSONArray("vulns")?.let { arr ->
                    for (i in 0 until arr.length()) cves += arr.optString(i)
                }

                val tags = mutableListOf<String>()
                json.optJSONArray("tags")?.let { arr ->
                    for (i in 0 until arr.length()) tags += arr.optString(i)
                }

                ShodanResult(
                    ip = json.optString("ip", ip),
                    hostnames = hostnames,
                    ports = ports,
                    cves = cves,
                    tags = tags,
                    vulns = cves
                )
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            ShodanResult(ip = ip, error = e.message ?: "Request failed")
        }
    }
}
