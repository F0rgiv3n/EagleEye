package com.eagleeye.modules.iot

import com.eagleeye.data.SsdpDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.*

/**
 * Sends an SSDP M-SEARCH multicast and collects UPnP device responses.
 * Works on LAN without root.
 */
object SsdpScanner {

    private const val SSDP_MULTICAST = "239.255.255.250"
    private const val SSDP_PORT      = 1900
    private const val TIMEOUT_MS     = 3000

    private val MSEARCH = """
        M-SEARCH * HTTP/1.1
        HOST: $SSDP_MULTICAST:$SSDP_PORT
        MAN: "ssdp:discover"
        MX: 2
        ST: ssdp:all

    """.trimIndent().replace("\n", "\r\n")

    suspend fun scan(): List<SsdpDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<SsdpDevice>()
        try {
            val socket = MulticastSocket(SSDP_PORT).apply {
                soTimeout = TIMEOUT_MS
                reuseAddress = true
            }
            val group = InetAddress.getByName(SSDP_MULTICAST)
            val packet = DatagramPacket(
                MSEARCH.toByteArray(),
                MSEARCH.length,
                group,
                SSDP_PORT
            )
            socket.send(packet)

            val buf = ByteArray(2048)
            val response = DatagramPacket(buf, buf.size)
            while (true) {
                try {
                    socket.receive(response)
                    val text = String(response.data, 0, response.length)
                    val ip = response.address.hostAddress ?: continue
                    parseSsdpResponse(ip, text)?.let {
                        if (devices.none { d -> d.ip == ip && d.st == it.st }) {
                            devices.add(it)
                        }
                    }
                } catch (_: SocketTimeoutException) { break }
            }
            socket.close()
        } catch (_: Exception) {}
        devices
    }

    private fun parseSsdpResponse(ip: String, text: String): SsdpDevice? {
        val headers = text.lines().associate { line ->
            val idx = line.indexOf(':')
            if (idx > 0) line.substring(0, idx).trim().uppercase() to line.substring(idx + 1).trim()
            else "" to ""
        }
        val location = headers["LOCATION"] ?: ""
        val server   = headers["SERVER"]   ?: ""
        val usn      = headers["USN"]      ?: ""
        val st       = headers["ST"]       ?: ""

        if (server.isBlank() && location.isBlank()) return null
        return SsdpDevice(ip = ip, server = server, location = location, usn = usn, st = st)
    }
}
