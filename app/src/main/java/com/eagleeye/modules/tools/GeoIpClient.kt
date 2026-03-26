package com.eagleeye.modules.tools

import com.eagleeye.data.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeoIpClient {

    /** Resolve the device's own public IP and location. */
    suspend fun resolveHome(): GeoPoint? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("http://ip-api.com/json?fields=query,status,lat,lon,country,countryCode,city,isp")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 6000; conn.readTimeout = 6000
            val obj = JSONObject(conn.inputStream.bufferedReader().readText())
            if (obj.optString("status") == "success") parsePoint(obj, isHome = true) else null
        } catch (_: Exception) { null }
    }

    /** Batch-resolve up to 100 IPs. Returns only successful results. */
    suspend fun resolveIps(ips: List<String>): List<GeoPoint> = withContext(Dispatchers.IO) {
        if (ips.isEmpty()) return@withContext emptyList()
        try {
            val conn = URL("http://ip-api.com/batch?fields=query,status,lat,lon,country,countryCode,city,isp")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            conn.outputStream.write(JSONArray(ips.take(100)).toString().toByteArray())
            val arr = JSONArray(conn.inputStream.bufferedReader().readText())
            (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .filter { it.optString("status") == "success" }
                .map { parsePoint(it) }
        } catch (_: Exception) { emptyList() }
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
