package com.eagleeye.data

/**
 * Synthetic data used when [AppSettings.demoMode] is enabled. Lets us produce
 * realistic portfolio screenshots without exposing real network state.
 *
 * All MAC addresses use the IETF-reserved 00:00:5E prefix and documentation
 * vendor names so they are unambiguously fake.
 */
object DemoOverrides {

    private val now = System.currentTimeMillis()

    val lanDevices: List<LanDevice> = listOf(
        LanDevice(
            mac = "00:00:5E:00:53:01",
            ip = "192.168.1.1",
            hostname = "router.lan",
            vendor = "TP-Link Technologies",
            isOnline = true, latencyMs = 2,
            openPorts = "22,53,80,443",
            firstSeen = now - 86_400_000L * 30,
            lastSeen = now,
            isKnown = true,
            alias = "Home Router"
        ),
        LanDevice(
            mac = "00:00:5E:00:53:02",
            ip = "192.168.1.10",
            hostname = "macbook-pro.lan",
            vendor = "Apple, Inc.",
            isOnline = true, latencyMs = 8,
            openPorts = "22,5000",
            firstSeen = now - 86_400_000L * 14,
            lastSeen = now,
            isKnown = true,
            alias = "Workstation"
        ),
        LanDevice(
            mac = "00:00:5E:00:53:03",
            ip = "192.168.1.21",
            hostname = "pixel-7.lan",
            vendor = "Google LLC",
            isOnline = true, latencyMs = 12,
            openPorts = "",
            firstSeen = now - 86_400_000L * 7,
            lastSeen = now,
            isKnown = true,
            alias = "Phone"
        ),
        LanDevice(
            mac = "00:00:5E:00:53:04",
            ip = "192.168.1.32",
            hostname = "nas.lan",
            vendor = "Synology Inc.",
            isOnline = true, latencyMs = 4,
            openPorts = "22,80,443,5000,5001",
            firstSeen = now - 86_400_000L * 60,
            lastSeen = now,
            isKnown = true,
            alias = "NAS"
        ),
        LanDevice(
            mac = "00:00:5E:00:53:05",
            ip = "192.168.1.45",
            hostname = "",
            vendor = "Hangzhou Hikvision",
            isOnline = true, latencyMs = 18,
            openPorts = "80,554,8000",
            firstSeen = now - 86_400_000L * 2,
            lastSeen = now,
            isKnown = false,
            alias = ""
        ),
        LanDevice(
            mac = "00:00:5E:00:53:06",
            ip = "192.168.1.58",
            hostname = "raspberrypi.lan",
            vendor = "Raspberry Pi Foundation",
            isOnline = true, latencyMs = 6,
            openPorts = "22,1883,8123",
            firstSeen = now - 86_400_000L * 21,
            lastSeen = now,
            isKnown = true,
            alias = "Home Assistant"
        ),
        LanDevice(
            mac = "00:00:5E:00:53:07",
            ip = "192.168.1.71",
            hostname = "",
            vendor = "Unknown",
            isOnline = true, latencyMs = 28,
            openPorts = "23,80,2323",
            firstSeen = now - 86_400_000L * 1,
            lastSeen = now,
            isKnown = false,
            alias = ""
        ),
        LanDevice(
            mac = "00:00:5E:00:53:08",
            ip = "192.168.1.84",
            hostname = "lg-tv.lan",
            vendor = "LG Electronics",
            isOnline = false, latencyMs = 0,
            openPorts = "",
            firstSeen = now - 86_400_000L * 90,
            lastSeen = now - 3_600_000L * 6,
            isKnown = true,
            alias = "Living Room TV"
        ),
    )

    val networkEvents: List<NetworkEvent> = listOf(
        NetworkEvent(
            id = 1,
            type = EventType.NEW_DEVICE,
            severity = EventSeverity.HIGH,
            title = "New device on LAN",
            detail = "Unknown vendor at 192.168.1.71 (00:00:5E:00:53:07). Ports 23, 80, 2323 open — looks like a default-credentials IoT device.",
            timestamp = now - 60_000L * 3,
            ip = "192.168.1.71",
            mac = "00:00:5E:00:53:07"
        ),
        NetworkEvent(
            id = 2,
            type = EventType.SECURITY_AUDIT,
            severity = EventSeverity.MEDIUM,
            title = "Security audit completed",
            detail = "2 threats detected · score 78/100",
            timestamp = now - 60_000L * 12
        ),
        NetworkEvent(
            id = 3,
            type = EventType.WPS_DETECTED,
            severity = EventSeverity.HIGH,
            title = "WPS enabled on access point",
            detail = "Your router still has WPS PIN enabled. Disable it in the admin panel to prevent Pixie Dust attacks.",
            timestamp = now - 60_000L * 35,
            ssid = "HomeNet"
        ),
        NetworkEvent(
            id = 4,
            type = EventType.DNS_CHANGED,
            severity = EventSeverity.MEDIUM,
            title = "DNS server changed",
            detail = "Primary DNS changed from 192.168.1.1 to 1.1.1.1 since last scan.",
            timestamp = now - 60_000L * 47
        ),
        NetworkEvent(
            id = 5,
            type = EventType.SCAN_COMPLETE,
            severity = EventSeverity.INFO,
            title = "LAN scan complete",
            detail = "8 devices discovered · 7 online · 2 unknown",
            timestamp = now - 60_000L * 60
        ),
        NetworkEvent(
            id = 6,
            type = EventType.MONITOR_STARTED,
            severity = EventSeverity.INFO,
            title = "Background monitor started",
            detail = "Scan interval: 15 min · Notifications: ON",
            timestamp = now - 60_000L * 73
        ),
        NetworkEvent(
            id = 7,
            type = EventType.NEW_DEVICE,
            severity = EventSeverity.MEDIUM,
            title = "New device on LAN",
            detail = "Hangzhou Hikvision camera at 192.168.1.45.",
            timestamp = now - 60_000L * 95,
            ip = "192.168.1.45",
            mac = "00:00:5E:00:53:05"
        ),
        NetworkEvent(
            id = 8,
            type = EventType.OPEN_NETWORK,
            severity = EventSeverity.LOW,
            title = "Open network nearby",
            detail = "\"FreeWifi-Guest\" detected without encryption. Avoid connecting without a VPN.",
            timestamp = now - 60_000L * 120,
            ssid = "FreeWifi-Guest"
        ),
    )

    val threats: List<Threat> = listOf(
        Threat(
            id = "wps_demo",
            level = ThreatLevel.HIGH,
            title = "WPS Enabled",
            description = "Wi-Fi Protected Setup (WPS) is enabled on this network. WPS PIN attacks (Pixie Dust) can compromise the router in minutes.",
            recommendation = "Disable WPS in your router admin panel."
        ),
        Threat(
            id = "unknown_dev_demo",
            level = ThreatLevel.MEDIUM,
            title = "Unknown Device on LAN",
            description = "An unidentified device (00:00:5E:00:53:07) with telnet exposed appeared on your network 3 minutes ago.",
            recommendation = "Verify the device. If unknown, isolate it on a guest VLAN."
        ),
    )

    val securityScore: SecurityScore = SecurityScore(
        total = 78,
        encryption = 20,   // WPA2 (not WPA3)
        noWps = 0,         // -15: WPS still on
        noEvilTwin = 15,
        dnsIntegrity = 10,
        noUnknownDevices = 0, // -15: hikvision + unknown IoT
        noOpenPorts = 15,
        threats = threats
    )

    val wifiConnection: WifiConnectionInfo = WifiConnectionInfo(
        ssid = "HomeNet-5G",
        bssid = "00:00:5E:00:53:01",
        ipAddress = "192.168.1.42",
        gateway = "192.168.1.1",
        subnetMask = "255.255.255.0",
        dns1 = "1.1.1.1",
        dns2 = "8.8.8.8",
        linkSpeedMbps = 433,
        rssi = -52,
        frequencyMhz = 5180,
        securityType = "WPA2-PSK",
        isConnected = true
    )

    val scannedNetworks: List<ScannedNetwork> = listOf(
        ScannedNetwork("HomeNet-5G",        "00:00:5E:00:53:01", -52, 5180, "WPA2"),
        ScannedNetwork("HomeNet-2.4",       "00:00:5E:00:53:02", -55, 2462, "WPA2"),
        ScannedNetwork("Office-Guest",      "00:00:5E:00:53:10", -67, 2412, "WPA2"),
        ScannedNetwork("Cafe-WiFi",         "00:00:5E:00:53:20", -71, 2437, "OPEN"),
        ScannedNetwork("Neighbour",         "00:00:5E:00:53:30", -74, 5240, "WPA3"),
        ScannedNetwork("PrintServer",       "00:00:5E:00:53:40", -78, 2452, "WPA2"),
        ScannedNetwork("FreeWifi-Guest",    "00:00:5E:00:53:50", -82, 2412, "OPEN"),
        ScannedNetwork("",                  "00:00:5E:00:53:60", -85, 5300, "WPA2", isHidden = true)
    )

    val signalHistory: List<SignalSample> = run {
        val now = System.currentTimeMillis()
        val rssiSamples = listOf(-68, -65, -62, -58, -55, -54, -56, -53, -50, -52, -54, -55, -52, -51, -50, -52, -53, -52)
        rssiSamples.mapIndexed { i, rssi ->
            SignalSample(timestamp = now - (rssiSamples.size - i) * 5_000L, rssi = rssi, ssid = "HomeNet-5G")
        }
    }

    /** Fake current MAC address shown in the MAC screen. */
    const val deviceMac: String = "00:00:5E:00:53:99"

    val macInfo: MacInfo = MacInfo(
        currentMac = "02:1A:7D:8B:F4:99",   // locally-administered (random)
        type = MacType.RANDOMIZED,
        vendor = "Randomized (locally administered)",
        isRandomized = true,
        hardwareMac = "00:00:5E:00:53:00"   // synthetic hardware MAC
    )
}
