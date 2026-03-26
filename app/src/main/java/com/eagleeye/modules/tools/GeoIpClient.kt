package com.eagleeye.modules.tools

import com.eagleeye.data.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeoIpClient {

    /** Resolve the device's own public IP and location. Returns null on failure. */
    suspend fun resolveHome(): GeoPoint? = withContext(Dispatchers.IO) {
        val conn = URL("http://ip-api.com/json?fields=query,status,lat,lon,country,countryCode,city,isp")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 6000; conn.readTimeout = 6000
        try {
            val body = if (conn.responseCode == 200)
                conn.inputStream.bufferedReader().readText()
            else
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            val obj = JSONObject(body)
            if (obj.optString("status") == "success") parsePoint(obj, isHome = true) else null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        } finally { conn.disconnect() }
    }

    /** Batch-resolve up to 100 IPs. Returns only successful results. */
    suspend fun resolveIps(ips: List<String>): List<GeoPoint> = withContext(Dispatchers.IO) {
        if (ips.isEmpty()) return@withContext emptyList()
        val conn = URL("http://ip-api.com/batch?fields=query,status,lat,lon,country,countryCode,city,isp")
            .openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 8000; conn.readTimeout = 8000
        try {
            val body = JSONArray(ips.take(100)).toString().toByteArray()
            conn.outputStream.write(body)
            conn.outputStream.flush()
            val response = if (conn.responseCode == 200)
                conn.inputStream.bufferedReader().readText()
            else
                conn.errorStream?.bufferedReader()?.readText() ?: "[]"
            val arr = JSONArray(response)
            (0 until arr.length())
                .mapNotNull { arr.optJSONObject(it) }
                .filter { it.optString("status") == "success" }
                .map { parsePoint(it) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        } finally { conn.disconnect() }
    }

    private fun parsePoint(obj: JSONObject, isHome: Boolean = false) = GeoPoint(
        ip          = obj.optString("query"),
        lat         = obj.optDouble("lat", 0.0).toFloat(),
        lon         = obj.optDouble("lon", 0.0).toFloat(),
        country     = obj.optString("country"),
        countryCode = obj.optString("countryCode"),
        city        = obj.optString("city"),
        isp         = obj.optString("isp"),
        isHome      = isHome
    )
}
