package com.eagleeye.modules.lan

import org.junit.Assert.assertEquals
import org.junit.Test

class OuiLookupTest {

    @Test
    fun `matchPrefix returns Unknown for blank or short input`() {
        assertEquals("Unknown", matchPrefix(emptyMap(), ""))
        assertEquals("Unknown", matchPrefix(emptyMap(), "AA:BB"))
    }

    @Test
    fun `matchPrefix returns Unknown when no prefix matches`() {
        val cache = mapOf("00:11:22" to "Some Vendor")
        assertEquals("Unknown", matchPrefix(cache, "AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `matchPrefix matches a standard 24-bit OUI`() {
        val cache = mapOf("AA:BB:CC" to "Acme Corp")
        assertEquals("Acme Corp", matchPrefix(cache, "AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `matchPrefix prefers 36-bit MA-S over 24-bit OUI`() {
        // A vendor with both an MA-L registration (24-bit) and a MA-S sub-allocation (36-bit).
        // The longer match should win.
        val cache = mapOf(
            "AA:BB:CC" to "Generic Vendor",
            "AA:BB:CC:D" to "Sub-block Vendor",   // 28-bit
            "AA:BB:CC:DD:E" to "MA-S Vendor"      // 36-bit
        )
        assertEquals("MA-S Vendor", matchPrefix(cache, "AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `matchPrefix falls back from 36 to 28 to 24 bits`() {
        val cache = mapOf(
            "AA:BB:CC" to "Big Vendor",
            "AA:BB:CC:D" to "Sub-block Vendor"
        )
        // 36-bit prefix "AA:BB:CC:DD:E" not present → falls to 28-bit "AA:BB:CC:D"
        assertEquals("Sub-block Vendor", matchPrefix(cache, "AA:BB:CC:DD:EE:FF"))
        // For a MAC where the 28-bit prefix differs, falls to the 24-bit OUI.
        assertEquals("Big Vendor", matchPrefix(cache, "AA:BB:CC:99:00:00"))
    }

    @Test
    fun `matchPrefix is case-insensitive`() {
        val cache = mapOf("AA:BB:CC" to "Acme Corp")
        assertEquals("Acme Corp", matchPrefix(cache, "aa:bb:cc:dd:ee:ff"))
        assertEquals("Acme Corp", matchPrefix(cache, "Aa:Bb:Cc:Dd:Ee:Ff"))
    }

    @Test
    fun `matchPrefix accepts dash-separated MAC notation`() {
        val cache = mapOf("AA:BB:CC" to "Acme Corp")
        assertEquals("Acme Corp", matchPrefix(cache, "AA-BB-CC-DD-EE-FF"))
    }

    @Test
    fun `OuiLookup public lookup delegates to matchPrefix via seeded cache`() {
        OuiLookup.seedForTest(mapOf("DE:AD:BE" to "Test Vendor"))
        assertEquals("Test Vendor", OuiLookup.lookup("DE:AD:BE:EF:00:01"))
        assertEquals("Unknown", OuiLookup.lookup("11:22:33:44:55:66"))
    }
}
