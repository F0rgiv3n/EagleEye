package com.eagleeye.modules.cve

import com.eagleeye.data.CveEntry
import com.eagleeye.data.CveLookupResult
import com.eagleeye.data.CveSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Queries the NIST NVD API v2 for CVEs.
 * Public API — no key required for basic queries (rate limited to 5 req/30s).
 * https://nvd.nist.gov/developers/vulnerabilities
 */
class CveRepository {

    // Simple in-memory cache to avoid repeat API calls
    private val cache = mutableMapOf<String, CveLookupResult>()

    suspend fun searchCves(keyword: String, maxResults: Int = 10): CveLookupResult =
        withContext(Dispatchers.IO) {
            val cacheKey = keyword.lowercase().trim()
            cache[cacheKey]?.let { return@withContext it }

            return@withContext try {
                val encoded = URLEncoder.encode(keyword.trim(), "UTF-8")
                val url = URL("https://services.nvd.nist.gov/rest/json/cves/2.0?keywordSearch=$encoded&resultsPerPage=$maxResults")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "EagleEye/1.0")

                try {
                    if (conn.responseCode != 200) {
                        return@withContext CveLookupResult(keyword, emptyList(), 0, "HTTP ${conn.responseCode}")
                    }
                    val body = conn.inputStream.bufferedReader().readText()
                    val result = parseNvdResponse(keyword, body)
                    cache[cacheKey] = result
                    result
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                CveLookupResult(keyword, emptyList(), 0, e.message ?: "Network error")
            }
        }

    private fun parseNvdResponse(query: String, json: String): CveLookupResult {
        return try {
            val root = JSONObject(json)
            val total = root.optInt("totalResults", 0)
            val items = root.optJSONArray("vulnerabilities") ?: return CveLookupResult(query, emptyList(), total)

            val entries = mutableListOf<CveEntry>()
            for (i in 0 until items.length()) {
                val cve = items.getJSONObject(i).getJSONObject("cve")
                val id = cve.optString("id", "")

                // Description (English)
                val descriptions = cve.optJSONArray("descriptions")
                val description = (0 until (descriptions?.length() ?: 0))
                    .mapNotNull { descriptions?.optJSONObject(it) }
                    .firstOrNull { it.optString("lang") == "en" }
                    ?.optString("value", "No description") ?: "No description"

                // CVSS Score
                val metrics = cve.optJSONObject("metrics")
                val (score, severity) = extractCvss(metrics)

                // Published date
                val published = cve.optString("published", "").take(10)

                // References
                val refsArray = cve.optJSONArray("references")
                val refs = (0 until (refsArray?.length() ?: 0).coerceAtMost(3))
                    .mapNotNull { refsArray?.optJSONObject(it)?.optString("url", "") }
                    .filter { it.isNotBlank() }

                entries.add(CveEntry(id, description, score, severity, published, refs))
            }

            CveLookupResult(query, entries.sortedByDescending { it.cvssScore }, total)
        } catch (e: Exception) {
            CveLookupResult(query, emptyList(), 0, "Parse error: ${e.message}")
        }
    }

    private fun extractCvss(metrics: JSONObject?): Pair<Float, CveSeverity> {
        if (metrics == null) return 0f to CveSeverity.NONE

        // Try CVSS v3.1, then v3.0, then v2.0
        val v31 = metrics.optJSONArray("cvssMetricV31")?.optJSONObject(0)
            ?.optJSONObject("cvssData")
        val v30 = metrics.optJSONArray("cvssMetricV30")?.optJSONObject(0)
            ?.optJSONObject("cvssData")
        val v2 = metrics.optJSONArray("cvssMetricV2")?.optJSONObject(0)
            ?.optJSONObject("cvssData")

        val data = v31 ?: v30 ?: v2 ?: return 0f to CveSeverity.NONE
        val score = data.optDouble("baseScore", 0.0).toFloat()
        val sev = when {
            score >= 9.0 -> CveSeverity.CRITICAL
            score >= 7.0 -> CveSeverity.HIGH
            score >= 4.0 -> CveSeverity.MEDIUM
            score > 0.0  -> CveSeverity.LOW
            else         -> CveSeverity.NONE
        }
        return score to sev
    }
}
