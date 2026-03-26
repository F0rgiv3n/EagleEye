package com.eagleeye.modules.packet

import com.eagleeye.data.CapturedPacket
import com.eagleeye.data.IpProtocol

object PacketParser {

    fun parse(buf: ByteArray, len: Int): CapturedPacket? {
        if (len < 20) return null
        val version = (buf[0].toInt() and 0xFF) shr 4
        if (version != 4) return null

        val ihl = (buf[0].toInt() and 0x0F) * 4
        if (ihl < 20 || ihl >= len) return null

        val totalLen = ((buf[2].toInt() and 0xFF) shl 8) or (buf[3].toInt() and 0xFF)
        val protoByte = buf[9].toInt() and 0xFF

        val srcIp = formatIp(buf, 12)
        val dstIp = formatIp(buf, 16)

        val protocol = when (protoByte) {
            6  -> IpProtocol.TCP
            17 -> IpProtocol.UDP
            1  -> IpProtocol.ICMP
            else -> IpProtocol.OTHER
        }

        var srcPort = 0
        var dstPort = 0
        var dnsQuery = ""
        var info = ""

        if ((protocol == IpProtocol.TCP || protocol == IpProtocol.UDP) && ihl + 4 <= len) {
            srcPort = ((buf[ihl].toInt() and 0xFF) shl 8) or (buf[ihl + 1].toInt() and 0xFF)
            dstPort = ((buf[ihl + 2].toInt() and 0xFF) shl 8) or (buf[ihl + 3].toInt() and 0xFF)
            info = getServiceName(dstPort).ifEmpty { getServiceName(srcPort) }

            if (protocol == IpProtocol.UDP && (dstPort == 53 || srcPort == 53)) {
                val udpHeaderLen = 8
                val dnsOffset = ihl + udpHeaderLen
                if (dnsOffset + 12 < len) {
                    dnsQuery = try { parseDnsQuery(buf, dnsOffset, len) } catch (_: Exception) { "" }
                    info = "DNS"
                }
            }
        }

        return CapturedPacket(
            protocol = protocol,
            srcIp = srcIp,
            dstIp = dstIp,
            srcPort = srcPort,
            dstPort = dstPort,
            size = totalLen.coerceAtMost(len),
            dnsQuery = dnsQuery,
            info = info
        )
    }

    private fun formatIp(buf: ByteArray, offset: Int): String {
        if (offset + 3 >= buf.size) return "0.0.0.0"
        return "${buf[offset].toInt() and 0xFF}.${buf[offset + 1].toInt() and 0xFF}" +
               ".${buf[offset + 2].toInt() and 0xFF}.${buf[offset + 3].toInt() and 0xFF}"
    }

    private fun parseDnsQuery(buf: ByteArray, dnsOffset: Int, len: Int): String {
        // DNS header is 12 bytes; question section starts at dnsOffset + 12
        var pos = dnsOffset + 12
        val labels = mutableListOf<String>()
        while (pos < len) {
            val labelLen = buf[pos].toInt() and 0xFF
            if (labelLen == 0) break
            pos++
            if (pos + labelLen > len) break
            labels.add(String(buf, pos, labelLen, Charsets.US_ASCII))
            pos += labelLen
        }
        return labels.joinToString(".")
    }

    private fun getServiceName(port: Int): String = when (port) {
        80    -> "HTTP"
        443   -> "HTTPS"
        53    -> "DNS"
        22    -> "SSH"
        25    -> "SMTP"
        587   -> "SMTP"
        993   -> "IMAPS"
        995   -> "POP3S"
        143   -> "IMAP"
        110   -> "POP3"
        3306  -> "MySQL"
        5432  -> "PostgreSQL"
        6379  -> "Redis"
        27017 -> "MongoDB"
        8080  -> "HTTP-Alt"
        8443  -> "HTTPS-Alt"
        else  -> ""
    }
}
