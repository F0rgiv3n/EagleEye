package com.eagleeye.modules.tools

import com.eagleeye.data.FirewallTestResult
import com.eagleeye.data.PortBlockResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class FirewallTester {

    companion object {
        val TEST_PORTS = listOf(
            21   to "FTP",
            22   to "SSH",
            23   to "Telnet",
            25   to "SMTP",
            53   to "DNS",
            80   to "HTTP",
            110  to "POP3",
            143  to "IMAP",
            443  to "HTTPS",
            465  to "SMTPS",
            587  to "SMTP/TLS",
            993  to "IMAPS",
            1194 to "OpenVPN",
            1723 to "PPTP",
            3389 to "RDP",
            5228 to "FCM/GCM",
            8080 to "HTTP-Alt",
            8443 to "HTTPS-Alt"
        )
        private const val TEST_HOST = "portquiz.net"
    }

    suspend fun run(onProgress: (done: Int, total: Int) -> Unit): FirewallTestResult =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<PortBlockResult>()
            TEST_PORTS.forEachIndexed { idx, (port, service) ->
                onProgress(idx, TEST_PORTS.size)
                results += testPort(port, service)
            }
            onProgress(TEST_PORTS.size, TEST_PORTS.size)
            FirewallTestResult(
                results      = results,
                openCount    = results.count { it.isOpen },
                blockedCount = results.count { !it.isOpen },
                testHost     = TEST_HOST
            )
        }

    private fun testPort(port: Int, service: String): PortBlockResult {
        return try {
            val socket = Socket()
            val start  = System.currentTimeMillis()
            socket.connect(InetSocketAddress(TEST_HOST, port), 3000)
            val latency = System.currentTimeMillis() - start
            socket.close()
            PortBlockResult(port, service, true, latency)
        } catch (_: Exception) {
            PortBlockResult(port, service, false, -1L)
        }
    }
}
