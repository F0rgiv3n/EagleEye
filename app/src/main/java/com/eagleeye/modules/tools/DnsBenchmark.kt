package com.eagleeye.modules.tools

import com.eagleeye.data.DnsBenchmarkResult
import com.eagleeye.data.DnsServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsBenchmark {

    private val servers = listOf(
        "8.8.8.8"         to "Google",
        "1.1.1.1"         to "Cloudflare",
        "9.9.9.9"         to "Quad9",
        "208.67.222.222"  to "OpenDNS",
        "8.8.4.4"         to "Google Alt",
        "1.0.0.1"         to "Cloudflare Alt"
    )

    // Minimal DNS A-record query for "google.com"
    private val dnsQuery = byteArrayOf(
        0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x06, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65,   // "google"
        0x03, 0x63, 0x6f, 0x6d,                       // "com"
        0x00, 0x00, 0x01, 0x00, 0x01
    )

    suspend fun run(): DnsBenchmarkResult = withContext(Dispatchers.IO) {
        val raw = servers.map { (ip, name) -> benchmarkServer(ip, name) }
        val sorted = raw.sortedWith(
            compareByDescending<DnsServerResult> { it.successRate }
                .thenBy { it.avgMs }
        )
        val ranked = sorted.mapIndexed { i, r -> r.copy(rank = i + 1) }
        DnsBenchmarkResult(results = ranked, fastest = ranked.firstOrNull()?.name ?: "")
    }

    private fun benchmarkServer(ip: String, name: String): DnsServerResult {
        val times   = mutableListOf<Long>()
        var success = 0
        repeat(5) {
            val socket = DatagramSocket()
            socket.soTimeout = 2000
            try {
                val addr  = InetAddress.getByName(ip)
                val send  = DatagramPacket(dnsQuery, dnsQuery.size, addr, 53)
                val recv  = DatagramPacket(ByteArray(512), 512)
                val start = System.currentTimeMillis()
                socket.send(send)
                socket.receive(recv)
                times += System.currentTimeMillis() - start
                success++
            } catch (_: Exception) {
            } finally {
                socket.close()
            }
        }
        return DnsServerResult(
            server      = ip,
            name        = name,
            avgMs       = if (times.isEmpty()) 9999L else times.average().toLong(),
            minMs       = times.minOrNull() ?: 9999L,
            maxMs       = times.maxOrNull() ?: 9999L,
            successRate = success * 20   // out of 5 probes → 0/20/40/60/80/100
        )
    }
}
