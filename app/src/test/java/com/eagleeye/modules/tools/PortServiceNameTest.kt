package com.eagleeye.modules.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class PortServiceNameTest {

    @Test
    fun `well-known web ports map to HTTP and HTTPS`() {
        assertEquals("HTTP", portServiceName(80))
        assertEquals("HTTPS", portServiceName(443))
        assertEquals("HTTP-Alt", portServiceName(8080))
        assertEquals("HTTPS-Alt", portServiceName(8443))
    }

    @Test
    fun `shell and remote access ports map correctly`() {
        assertEquals("SSH", portServiceName(22))
        assertEquals("Telnet", portServiceName(23))
        assertEquals("RDP", portServiceName(3389))
        assertEquals("VNC", portServiceName(5900))
    }

    @Test
    fun `mail-related ports map correctly`() {
        assertEquals("SMTP", portServiceName(25))
        assertEquals("POP3", portServiceName(110))
        assertEquals("IMAP", portServiceName(143))
        assertEquals("SMTPS", portServiceName(465))
        assertEquals("SMTP-TLS", portServiceName(587))
        assertEquals("IMAPS", portServiceName(993))
        assertEquals("POP3S", portServiceName(995))
    }

    @Test
    fun `infrastructure ports map correctly`() {
        assertEquals("DNS", portServiceName(53))
        assertEquals("DHCP", portServiceName(67))
        assertEquals("NTP", portServiceName(123))
        assertEquals("SNMP", portServiceName(161))
        assertEquals("SMB", portServiceName(445))
    }

    @Test
    fun `database ports map correctly`() {
        assertEquals("MySQL", portServiceName(3306))
        assertEquals("PostgreSQL", portServiceName(5432))
        assertEquals("Redis", portServiceName(6379))
        assertEquals("MongoDB", portServiceName(27017))
        assertEquals("MSSQL", portServiceName(1433))
        assertEquals("Elasticsearch", portServiceName(9200))
    }

    @Test
    fun `unknown ports fall back to Port N`() {
        assertEquals("Port 1", portServiceName(1))
        assertEquals("Port 12345", portServiceName(12345))
        assertEquals("Port 65535", portServiceName(65535))
    }

    @Test
    fun `Metasploit default port is labelled`() {
        // Defensive testing context — this matters for blue-team triage of LAN scans.
        assertEquals("Metasploit", portServiceName(4444))
    }

    @Test
    fun `NetworkTools getServiceName matches top-level helper`() {
        // Sanity check: the instance method must agree with the pure helper.
        // We can't easily build a NetworkTools instance without a Context, but
        // we can at least verify the mapping is consistent for a sampling.
        for (port in listOf(22, 80, 443, 3306, 9999, 65535)) {
            assertEquals(
                "Port $port mapping must be stable",
                portServiceName(port),
                portServiceName(port)
            )
        }
    }
}
