package com.eagleeye.modules.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsAnalysisTest {

    // ── intToIp ────────────────────────────────────────────────────────────

    @Test
    fun `intToIp decodes little-endian Android DhcpInfo packing`() {
        // 1.1.1.1 packed little-endian: 0x01010101
        assertEquals("1.1.1.1", intToIp(0x01010101))
        // 8.8.8.8
        assertEquals("8.8.8.8", intToIp(0x08080808))
        // 192.168.1.1 → byte order reversed: 0x0101A8C0
        assertEquals("192.168.1.1", intToIp(0x0101A8C0))
    }

    @Test
    fun `intToIp handles zero address`() {
        assertEquals("0.0.0.0", intToIp(0))
    }

    @Test
    fun `intToIp handles max bytes`() {
        // 255.255.255.255
        assertEquals("255.255.255.255", intToIp(-1))
    }

    // ── isPublicDns ────────────────────────────────────────────────────────

    @Test
    fun `isPublicDns flags well-known public resolvers`() {
        assertTrue(isPublicDns("8.8.8.8"))
        assertTrue(isPublicDns("1.1.1.1"))
        assertTrue(isPublicDns("9.9.9.9"))
    }

    @Test
    fun `isPublicDns rejects RFC1918 private ranges`() {
        assertFalse(isPublicDns("10.0.0.1"))
        assertFalse(isPublicDns("10.255.255.254"))
        assertFalse(isPublicDns("172.16.0.1"))
        assertFalse(isPublicDns("172.31.0.1"))
        assertFalse(isPublicDns("192.168.0.1"))
        assertFalse(isPublicDns("192.168.1.254"))
    }

    @Test
    fun `isPublicDns accepts 172 addresses outside the private slice`() {
        // 172.15 and 172.32 are public per the RFC1918 definition (172.16-31 are private)
        assertTrue(isPublicDns("172.15.0.1"))
        assertTrue(isPublicDns("172.32.0.1"))
    }

    @Test
    fun `isPublicDns rejects malformed input`() {
        assertFalse(isPublicDns(""))
        assertFalse(isPublicDns("not-an-ip"))
        assertFalse(isPublicDns("8.8.8"))
        assertFalse(isPublicDns("8.8.8.8.8"))
    }

    // ── isKnownGoodDns ─────────────────────────────────────────────────────

    @Test
    fun `isKnownGoodDns recognises Google Cloudflare Quad9 OpenDNS`() {
        assertTrue(isKnownGoodDns("8.8.8.8"))
        assertTrue(isKnownGoodDns("8.8.4.4"))
        assertTrue(isKnownGoodDns("1.1.1.1"))
        assertTrue(isKnownGoodDns("1.0.0.1"))
        assertTrue(isKnownGoodDns("9.9.9.9"))
        assertTrue(isKnownGoodDns("208.67.222.222"))
    }

    @Test
    fun `isKnownGoodDns rejects unknown resolvers`() {
        assertFalse(isKnownGoodDns("4.4.4.4"))
        assertFalse(isKnownGoodDns("192.168.1.1"))
        assertFalse(isKnownGoodDns(""))
    }

    // ── findEvilTwinBssids ─────────────────────────────────────────────────

    @Test
    fun `findEvilTwinBssids returns empty when only one BSSID broadcasts SSID`() {
        val scans = listOf(
            ScanInfo("HomeWifi", "aa:aa:aa:aa:aa:aa"),
            ScanInfo("Neighbor", "bb:bb:bb:bb:bb:bb")
        )
        assertEquals(emptyList<String>(), findEvilTwinBssids(scans, "HomeWifi", "aa:aa:aa:aa:aa:aa"))
    }

    @Test
    fun `findEvilTwinBssids flags duplicate SSID with different BSSID`() {
        val scans = listOf(
            ScanInfo("HomeWifi", "aa:aa:aa:aa:aa:aa"),
            ScanInfo("HomeWifi", "cc:cc:cc:cc:cc:cc"),  // rogue
            ScanInfo("Neighbor", "bb:bb:bb:bb:bb:bb")
        )
        val rogues = findEvilTwinBssids(scans, "HomeWifi", "aa:aa:aa:aa:aa:aa")
        assertEquals(listOf("cc:cc:cc:cc:cc:cc"), rogues)
    }

    @Test
    fun `findEvilTwinBssids is case-insensitive on SSID`() {
        val scans = listOf(
            ScanInfo("MYNETWORK", "aa:aa:aa:aa:aa:aa"),
            ScanInfo("mynetwork", "dd:dd:dd:dd:dd:dd")
        )
        val rogues = findEvilTwinBssids(scans, "MyNetwork", "aa:aa:aa:aa:aa:aa")
        assertEquals(listOf("dd:dd:dd:dd:dd:dd"), rogues)
    }

    @Test
    fun `findEvilTwinBssids returns empty for blank inputs`() {
        val scans = listOf(ScanInfo("Foo", "aa:aa:aa:aa:aa:aa"))
        assertEquals(emptyList<String>(), findEvilTwinBssids(scans, "", "aa:aa:aa:aa:aa:aa"))
        assertEquals(emptyList<String>(), findEvilTwinBssids(scans, "Foo", ""))
    }

    @Test
    fun `findEvilTwinBssids flags multiple rogues at once`() {
        val scans = listOf(
            ScanInfo("Cafe", "aa:aa:aa:aa:aa:aa"),
            ScanInfo("Cafe", "bb:bb:bb:bb:bb:bb"),
            ScanInfo("Cafe", "cc:cc:cc:cc:cc:cc")
        )
        val rogues = findEvilTwinBssids(scans, "Cafe", "aa:aa:aa:aa:aa:aa")
        assertEquals(2, rogues.size)
        assertTrue("bb:bb:bb:bb:bb:bb" in rogues)
        assertTrue("cc:cc:cc:cc:cc:cc" in rogues)
    }
}
