package com.eagleeye.modules.portal

import com.eagleeye.data.CaptivePortalResult
import com.eagleeye.data.PortalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.SSLHandshakeException

/**
 * Detects and analyzes captive portals by probing known connectivity-check
 * endpoints and following redirect chains.
 */
class CaptivePortalDetector {

    // Endpoints that should return a known fixed response (no redirect = no portal)
    private val probeUrls = listOf(
        "http://connectivitycheck.gstatic.com/generate_204",
        "http://clients3.google.com/generate_204",
        "http://detectportal.firefox.com/success.txt",
        "http://www.msftconnecttest.com/connecttest.txt"
    )

    // Domains known to be legitimate portal providers
    private val knownLegitDomains = setOf(
        "gstatic.com", "google.com", "microsoft.com", "cisco.com",
        "aerohive.com", "ruckuswireless.com", "meraki.com", "aruba.com",
        "nomadix.com", "boingo.com", "ipass.com", "krome.net"
    )

    // Patterns that indicate a suspicious/phishing portal
    private val suspiciousPatterns = listOf(
        Regex("password.*required", RegexOption.IGNORE_CASE),
        Regex("enter.*credit.*card", RegexOption.IGNORE_CASE),
        Regex("verify.*identity", RegexOption.IGNORE_CASE),
        Regex("facebook.*login", RegexOption.IGNORE_CASE),
        Regex("paypal", RegexOption.IGNORE_CASE)
    )

    suspend fun detect(): CaptivePortalResult = withContext(Dispatchers.IO) {
        for (probeUrl in probeUrls) {
            val result = probe(probeUrl)
            if (result.status != PortalStatus.ERROR) return@withContext result
        }
        CaptivePortalResult(
            status = PortalStatus.ERROR,
            checkedUrl = probeUrls.first(),
            error = "All probe endpoints failed — no internet connectivity"
        )
    }

    private fun probe(probeUrl: String): CaptivePortalResult {
        val redirectChain = mutableListOf<String>()
        var currentUrl = probeUrl
        var responseCode = 0
        var hasCertIssue = false

        return try {
            repeat(8) { // Max 8 redirects
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                }

                responseCode = try { conn.responseCode } catch (e: SSLHandshakeException) {
                    hasCertIssue = true
                    -1
                }

                when {
                    // 204 = no content = no portal
                    responseCode == 204 -> {
                        conn.disconnect()
                        return@probe CaptivePortalResult(
                            status = PortalStatus.NONE,
                            checkedUrl = probeUrl,
                            responseCode = 204
                        )
                    }

                    // 200 on generate_204 = portal intercepted response
                    responseCode == 200 && probeUrl.contains("generate_204") -> {
                        val body = try {
                            conn.inputStream.bufferedReader().readText(500)
                        } catch (_: Exception) { "" }
                        val title = extractTitle(body)
                        val portalDomain = URL(currentUrl).host
                        val isLegit = knownLegitDomains.any { portalDomain.endsWith(it) }
                        val suspicionReasons = analyseSuspicion(body, currentUrl, hasCertIssue, isLegit)
                        conn.disconnect()
                        return@probe CaptivePortalResult(
                            status = if (suspicionReasons.isEmpty()) PortalStatus.DETECTED else PortalStatus.SUSPICIOUS,
                            portalUrl = currentUrl,
                            redirectChain = redirectChain,
                            pageTitle = title,
                            hasCertIssue = hasCertIssue,
                            isSuspicious = suspicionReasons.isNotEmpty(),
                            suspicionReasons = suspicionReasons,
                            responseCode = responseCode,
                            checkedUrl = probeUrl
                        )
                    }

                    // Redirect — follow it
                    responseCode in 301..399 -> {
                        val location = conn.getHeaderField("Location") ?: run {
                            conn.disconnect()
                            return@probe CaptivePortalResult(
                                status = PortalStatus.ERROR, checkedUrl = probeUrl,
                                error = "Redirect with no Location header"
                            )
                        }
                        redirectChain.add(currentUrl)
                        currentUrl = if (location.startsWith("http")) location
                                     else URL(URL(currentUrl), location).toString()
                        conn.disconnect()
                    }

                    else -> {
                        conn.disconnect()
                        return@probe CaptivePortalResult(
                            status = PortalStatus.ERROR, checkedUrl = probeUrl,
                            responseCode = responseCode,
                            error = "Unexpected response: $responseCode"
                        )
                    }
                }
            }
            // Exceeded max redirects — likely portal loop
            val portalDomain = runCatching { URL(currentUrl).host }.getOrDefault("unknown")
            val isLegit = knownLegitDomains.any { portalDomain.endsWith(it) }
            CaptivePortalResult(
                status = PortalStatus.SUSPICIOUS,
                portalUrl = currentUrl,
                redirectChain = redirectChain,
                hasCertIssue = hasCertIssue,
                isSuspicious = true,
                suspicionReasons = listOf("Excessive redirect chain (${redirectChain.size} hops)"),
                checkedUrl = probeUrl
            )
        } catch (e: Exception) {
            CaptivePortalResult(
                status = PortalStatus.ERROR,
                checkedUrl = probeUrl,
                error = e.message ?: "Connection failed"
            )
        }
    }

    private fun analyseSuspicion(
        body: String, url: String, hasCertIssue: Boolean, isKnownLegit: Boolean
    ): List<String> {
        val reasons = mutableListOf<String>()
        if (hasCertIssue) reasons.add("Invalid or self-signed SSL certificate")
        if (!isKnownLegit) {
            val domain = runCatching { URL(url).host }.getOrDefault("")
            if (domain.isNotBlank()) reasons.add("Unknown portal domain: $domain")
        }
        suspiciousPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(body)) {
                reasons.add("Suspicious content: \"${pattern.pattern}\"")
            }
        }
        if (url.startsWith("http://") && body.contains("password", ignoreCase = true)) {
            reasons.add("Password field on unencrypted HTTP page")
        }
        return reasons
    }

    private fun extractTitle(html: String): String {
        val match = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.getOrNull(1)?.trim()?.take(80) ?: ""
    }

    // Extension to read limited bytes to avoid huge downloads
    private fun java.io.InputStream.bufferedReader() =
        java.io.BufferedReader(java.io.InputStreamReader(this))

    private fun java.io.BufferedReader.readText(maxChars: Int): String {
        val sb = StringBuilder()
        var ch: Int
        var count = 0
        while (read().also { ch = it } != -1 && count++ < maxChars) sb.append(ch.toChar())
        return sb.toString()
    }
}
