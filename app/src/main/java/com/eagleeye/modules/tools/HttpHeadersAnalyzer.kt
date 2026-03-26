package com.eagleeye.modules.tools

import com.eagleeye.data.*
import java.net.HttpURLConnection
import java.net.URL

class HttpHeadersAnalyzer {

    private data class HeaderSpec(
        val name: String,
        val maxPoints: Int,
        val description: String,
        val check: (String) -> HeaderStatus
    )

    private val specs = listOf(
        HeaderSpec("Strict-Transport-Security", 20, "Forces HTTPS connections") { v ->
            val maxAge = Regex("max-age=(\\d+)").find(v)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: -1L
            when {
                maxAge < 0 -> HeaderStatus.MISSING
                maxAge < 15552000 -> HeaderStatus.WEAK
                else -> HeaderStatus.PRESENT
            }
        },
        HeaderSpec("Content-Security-Policy", 20, "Prevents XSS and injection attacks") { v ->
            when {
                v.isBlank() -> HeaderStatus.MISSING
                v.contains("unsafe-inline") || v.contains("unsafe-eval") -> HeaderStatus.WEAK
                else -> HeaderStatus.PRESENT
            }
        },
        HeaderSpec("X-Frame-Options", 10, "Prevents clickjacking") { v ->
            val upper = v.uppercase().trim()
            if (upper == "DENY" || upper == "SAMEORIGIN") HeaderStatus.PRESENT else HeaderStatus.MISSING
        },
        HeaderSpec("X-Content-Type-Options", 10, "Prevents MIME sniffing") { v ->
            if (v.lowercase().trim() == "nosniff") HeaderStatus.PRESENT else HeaderStatus.MISSING
        },
        HeaderSpec("Referrer-Policy", 10, "Controls referrer info leakage") { v ->
            when {
                v.isBlank() -> HeaderStatus.MISSING
                v == "unsafe-url" || v == "no-referrer-when-downgrade" -> HeaderStatus.WEAK
                else -> HeaderStatus.PRESENT
            }
        },
        HeaderSpec("Permissions-Policy", 10, "Restricts browser features") { v ->
            if (v.isNotBlank()) HeaderStatus.PRESENT else HeaderStatus.MISSING
        },
        HeaderSpec("Cross-Origin-Opener-Policy", 10, "Isolates browsing context") { v ->
            if (v.isNotBlank()) HeaderStatus.PRESENT else HeaderStatus.MISSING
        },
        HeaderSpec("Cross-Origin-Embedder-Policy", 10, "Controls cross-origin resources") { v ->
            if (v.isNotBlank()) HeaderStatus.PRESENT else HeaderStatus.MISSING
        }
    )

    suspend fun analyze(url: String): HttpHeadersResult {
        val normalizedUrl = if (!url.startsWith("http")) "https://$url" else url
        return try {
            val conn = openConnection(normalizedUrl, "HEAD")
            val responseCode = try { conn.responseCode } catch (e: Exception) { -1 }
            val headers = conn.headerFields ?: emptyMap()
            conn.disconnect()

            val headerMap = headers
                .filterKeys { it != null }
                .mapKeys { it.key.lowercase() }
                .mapValues { it.value.firstOrNull() ?: "" }

            buildResult(normalizedUrl, headerMap, responseCode)
        } catch (e: Exception) {
            try {
                val conn = openConnection(normalizedUrl, "GET")
                val responseCode = try { conn.responseCode } catch (ex: Exception) { -1 }
                val headers = conn.headerFields ?: emptyMap()
                conn.disconnect()

                val headerMap = headers
                    .filterKeys { it != null }
                    .mapKeys { it.key.lowercase() }
                    .mapValues { it.value.firstOrNull() ?: "" }

                buildResult(normalizedUrl, headerMap, responseCode)
            } catch (ex: Exception) {
                HttpHeadersResult(
                    url = normalizedUrl,
                    grade = HeaderGrade.ERROR,
                    score = 0,
                    headers = emptyList(),
                    infoLeaks = emptyList(),
                    error = ex.message ?: "Connection failed"
                )
            }
        }
    }

    private fun openConnection(url: String, method: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.instanceFollowRedirects = true
        conn.connect()
        return conn
    }

    private fun buildResult(url: String, headerMap: Map<String, String>, responseCode: Int): HttpHeadersResult {
        val entries = specs.map { spec ->
            val value = headerMap[spec.name.lowercase()] ?: ""
            val status = spec.check(value)
            val points = when (status) {
                HeaderStatus.PRESENT -> spec.maxPoints
                HeaderStatus.WEAK    -> spec.maxPoints / 2
                HeaderStatus.MISSING -> 0
            }
            SecurityHeaderEntry(
                name = spec.name,
                value = value,
                status = status,
                points = points,
                description = spec.description
            )
        }

        val totalMax = specs.sumOf { it.maxPoints }
        val earned = entries.sumOf { it.points }
        val score = (earned.toFloat() / totalMax * 100).toInt()

        val grade = when {
            score >= 90 -> HeaderGrade.A_PLUS
            score >= 70 -> HeaderGrade.A
            score >= 50 -> HeaderGrade.B
            score >= 30 -> HeaderGrade.C
            else        -> HeaderGrade.F
        }

        val infoLeaks = mutableListOf<String>()
        headerMap["server"]?.let { v ->
            if (v.isNotBlank() && (v.contains("/") || v.any { it.isDigit() })) {
                infoLeaks += "Server header exposes: $v"
            }
        }
        headerMap["x-powered-by"]?.let { v ->
            if (v.isNotBlank()) infoLeaks += "X-Powered-By exposes: $v"
        }
        headerMap["x-aspnet-version"]?.let { v ->
            if (v.isNotBlank()) infoLeaks += "ASP.NET version exposed: $v"
        }
        headerMap["x-aspnetmvc-version"]?.let { v ->
            if (v.isNotBlank()) infoLeaks += "ASP.NET MVC version exposed: $v"
        }

        return HttpHeadersResult(
            url = url,
            grade = grade,
            score = score,
            headers = entries,
            infoLeaks = infoLeaks,
            responseCode = responseCode
        )
    }
}
