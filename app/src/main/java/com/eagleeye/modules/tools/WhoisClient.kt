package com.eagleeye.modules.tools

import com.eagleeye.data.WhoisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket

class WhoisClient {

    suspend fun lookup(query: String): WhoisResult = withContext(Dispatchers.IO) {
        val cleaned = query.trim().removePrefix("https://").removePrefix("http://")
            .removePrefix("www.").substringBefore("/")
        val isIp = cleaned.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))

        // Reverse DNS always (quick)
        val reverseDns = runCatching {
            InetAddress.getByName(cleaned).canonicalHostName.takeIf { it != cleaned } ?: "—"
        }.getOrDefault("—")

        // WHOIS query
        val server = if (isIp) bestRirServer(cleaned) else "whois.iana.org"
        val raw = whoisQuery(cleaned, server)

        // If IANA redirects, follow referral
        val finalRaw = if (!isIp) {
            val referral = raw.lines()
                .firstOrNull { it.lowercase().startsWith("refer:") || it.lowercase().startsWith("whois:") }
                ?.substringAfter(":")?.trim()
            if (referral != null && referral.isNotBlank() && referral != server)
                whoisQuery(cleaned, referral).ifBlank { raw }
            else raw
        } else raw

        parseResult(query = cleaned, raw = finalRaw, reverseDns = reverseDns, isIp = isIp)
    }

    private fun whoisQuery(query: String, server: String): String {
        return try {
            Socket().use { socket ->
                socket.soTimeout = 8000
                socket.connect(java.net.InetSocketAddress(server, 43), 8000)
                PrintWriter(socket.outputStream, true).println("$query\r\n")
                BufferedReader(InputStreamReader(socket.inputStream))
                    .readText()
                    .take(8000)
            }
        } catch (_: Exception) { "" }
    }

    /** Pick the right RIR WHOIS server based on IP range. */
    private fun bestRirServer(ip: String): String {
        val first = ip.substringBefore(".").toIntOrNull() ?: 0
        val second = ip.substringAfter(".").substringBefore(".").toIntOrNull() ?: 0
        return when {
            first in 1..126 || first == 128 || first == 130 ||
                (first == 192 && second in 0..167) -> "whois.arin.net"          // ARIN (US/Canada)
            first in 193..195 || first in 212..213 ||
                first in 217..218 -> "whois.ripe.net"                           // RIPE (Europe)
            first in 58..61 || first in 110..126 ||
                first in 202..211 -> "whois.apnic.net"                          // APNIC (Asia-Pacific)
            first in 177..191 || first == 200 || first == 201 -> "whois.lacnic.net" // LACNIC (Latin Am)
            first in 41..41 || first in 102..102 || first == 196 -> "whois.afrinic.net" // AFRINIC
            else -> "whois.arin.net"
        }
    }

    private fun parseResult(query: String, raw: String, reverseDns: String, isIp: Boolean): WhoisResult {
        if (raw.isBlank()) return WhoisResult(
            query = query, reverseDns = reverseDns, error = "No response from WHOIS server"
        )

        fun field(vararg keys: String): String {
            for (key in keys) {
                val line = raw.lines().firstOrNull { line ->
                    line.trim().lowercase().startsWith("${key.lowercase()}:") &&
                        !line.trim().lowercase().startsWith("% ")
                } ?: continue
                val value = line.substringAfter(":").trim()
                if (value.isNotBlank()) return value
            }
            return ""
        }

        return WhoisResult(
            query      = query,
            reverseDns = reverseDns,
            org        = field("org-name", "OrgName", "organisation", "Organization", "descr", "owner"),
            country    = field("Country", "country"),
            netblock   = field("CIDR", "inetnum", "NetRange", "route"),
            registrar  = field("Registrar", "registrar"),
            created    = field("Creation Date", "RegDate", "created"),
            updated    = field("Updated Date", "Updated", "changed", "last-modified"),
            nameServer = raw.lines()
                .filter { it.trim().lowercase().startsWith("name server:") || it.trim().lowercase().startsWith("nserver:") }
                .take(3)
                .map { it.substringAfter(":").trim() }
                .filter { it.isNotBlank() },
            raw        = raw,
            isIpLookup = isIp
        )
    }
}
