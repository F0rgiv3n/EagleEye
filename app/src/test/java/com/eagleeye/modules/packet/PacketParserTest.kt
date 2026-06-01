package com.eagleeye.modules.packet

import com.eagleeye.data.IpProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure byte-array → CapturedPacket parser. No Android
 * dependencies, so these run on the host JVM via `./gradlew test`.
 */
class PacketParserTest {

    @Test
    fun `rejects buffers shorter than minimum IPv4 header`() {
        val buf = ByteArray(10)
        assertNull(PacketParser.parse(buf, 10))
    }

    @Test
    fun `rejects non-IPv4 packets`() {
        val buf = ByteArray(40)
        buf[0] = 0x60  // version=6, IHL=0
        assertNull(PacketParser.parse(buf, 40))
    }

    @Test
    fun `parses a minimal TCP packet`() {
        // 20-byte IPv4 header + 4-byte TCP src/dst port stub
        val buf = ByteArray(24)
        buf[0] = 0x45                                  // version=4, IHL=5 (20 bytes)
        buf[2] = 0x00; buf[3] = 0x18                   // total length = 24
        buf[9] = 6                                     // protocol = TCP
        // src IP 192.168.1.10
        buf[12] = 192.toByte(); buf[13] = 168.toByte(); buf[14] = 1; buf[15] = 10
        // dst IP 8.8.8.8
        buf[16] = 8; buf[17] = 8; buf[18] = 8; buf[19] = 8
        // src port 50000, dst port 443
        buf[20] = 0xC3.toByte(); buf[21] = 0x50.toByte()
        buf[22] = 0x01; buf[23] = 0xBB.toByte()

        val pkt = PacketParser.parse(buf, 24)
        assertNotNull(pkt)
        pkt!!
        assertEquals(IpProtocol.TCP, pkt.protocol)
        assertEquals("192.168.1.10", pkt.srcIp)
        assertEquals("8.8.8.8", pkt.dstIp)
        assertEquals(50000, pkt.srcPort)
        assertEquals(443, pkt.dstPort)
        assertEquals("HTTPS", pkt.info)
    }

    @Test
    fun `parses ICMP without ports`() {
        // 20-byte IPv4 header + minimum 8-byte ICMP echo header
        val buf = ByteArray(28)
        buf[0] = 0x45
        buf[2] = 0x00; buf[3] = 0x1C                   // total length = 28
        buf[9] = 1                                     // protocol = ICMP
        buf[12] = 10; buf[13] = 0; buf[14] = 0; buf[15] = 1
        buf[16] = 10; buf[17] = 0; buf[18] = 0; buf[19] = 2

        val pkt = PacketParser.parse(buf, 28)!!
        assertEquals(IpProtocol.ICMP, pkt.protocol)
        assertEquals(0, pkt.srcPort)
        assertEquals(0, pkt.dstPort)
    }

    @Test
    fun `extracts DNS query name from UDP packet on port 53`() {
        // IPv4 (20) + UDP (8) + DNS header (12) + question for "example.com" (13 bytes)
        val buf = ByteArray(20 + 8 + 12 + 13 + 4)
        buf[0] = 0x45
        buf[2] = 0x00; buf[3] = (buf.size and 0xFF).toByte()
        buf[9] = 17                                    // protocol = UDP
        buf[12] = 10; buf[15] = 1
        buf[16] = 8; buf[19] = 8
        // src port arbitrary, dst port 53
        buf[20] = 0xDE.toByte(); buf[21] = 0xAD.toByte()
        buf[22] = 0x00; buf[23] = 0x35.toByte()        // 53

        // DNS header at offset 28 (16 transaction-id + flags + counts = 12 bytes, leave zero)
        var p = 20 + 8 + 12  // question section
        buf[p++] = 7
        "example".forEachIndexed { i, c -> buf[p + i] = c.code.toByte() }
        p += 7
        buf[p++] = 3
        "com".forEachIndexed { i, c -> buf[p + i] = c.code.toByte() }
        p += 3
        buf[p] = 0                                     // terminator

        val pkt = PacketParser.parse(buf, buf.size)!!
        assertEquals(IpProtocol.UDP, pkt.protocol)
        assertEquals(53, pkt.dstPort)
        assertEquals("example.com", pkt.dnsQuery)
        assertEquals("DNS", pkt.info)
    }

    @Test
    fun `handles truncated TCP — header present but only 3 port bytes`() {
        // 20-byte IPv4 header + 3 stub bytes (parser needs >=4 to read ports)
        val buf = ByteArray(23)
        buf[0] = 0x45
        buf[2] = 0x00; buf[3] = 0x17                   // total length = 23
        buf[9] = 6                                     // TCP
        val pkt = PacketParser.parse(buf, 23)
        assertNotNull(pkt)
        assertEquals(0, pkt!!.srcPort)
        assertEquals(0, pkt.dstPort)
    }

    @Test
    fun `marks unknown protocol as OTHER`() {
        val buf = ByteArray(24)
        buf[0] = 0x45
        buf[2] = 0x00; buf[3] = 0x18                   // total length = 24
        buf[9] = 99.toByte()                           // unknown protocol number
        val pkt = PacketParser.parse(buf, 24)!!
        assertEquals(IpProtocol.OTHER, pkt.protocol)
    }

    @Test
    fun `size is clamped to received bytes`() {
        val buf = ByteArray(40)
        buf[0] = 0x45
        // total-length field says 1500 but len is only 40
        buf[2] = 0x05.toByte(); buf[3] = 0xDC.toByte()
        buf[9] = 6
        val pkt = PacketParser.parse(buf, 40)!!
        assertTrue("size must not exceed actual bytes received", pkt.size <= 40)
    }
}
