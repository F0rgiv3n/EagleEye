package com.eagleeye.data

data class WifiConnectionInfo(
    val ssid: String = "",
    val bssid: String = "",
    val ipAddress: String = "",
    val gateway: String = "",
    val subnetMask: String = "",
    val dns1: String = "",
    val dns2: String = "",
    val linkSpeedMbps: Int = 0,
    val rssi: Int = 0,
    val frequencyMhz: Int = 0,
    val securityType: String = "",
    val isConnected: Boolean = false
) {
    val channel: Int get() = frequencyToChannel(frequencyMhz)
    val signalStrength: SignalStrength get() = when {
        rssi >= -50 -> SignalStrength.EXCELLENT
        rssi >= -65 -> SignalStrength.GOOD
        rssi >= -75 -> SignalStrength.FAIR
        rssi >= -85 -> SignalStrength.WEAK
        else -> SignalStrength.NONE
    }

    val band: String get() = when {
        frequencyMhz in 2400..2500 -> "2.4 GHz"
        frequencyMhz in 5000..5900 -> "5 GHz"
        frequencyMhz in 5900..7200 -> "6 GHz"
        else -> "Unknown"
    }

    val signalPercent: Int get() {
        val clamped = rssi.coerceIn(-100, -30)
        return ((clamped + 100) * 100 / 70).coerceIn(0, 100)
    }
}

enum class SignalStrength(val label: String, val bars: Int) {
    EXCELLENT("Excellent", 4),
    GOOD("Good", 3),
    FAIR("Fair", 2),
    WEAK("Weak", 1),
    NONE("No Signal", 0)
}

data class ScannedNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val securityType: String,
    val isHidden: Boolean = false
) {
    val channel: Int get() = frequencyToChannel(frequencyMhz)
    val band: String get() = when {
        frequencyMhz in 2400..2500 -> "2.4G"
        frequencyMhz in 5000..5900 -> "5G"
        frequencyMhz in 5900..7200 -> "6G"
        else -> "?"
    }
    val securityGrade: SecurityGrade get() = when {
        securityType.contains("WPA3") -> SecurityGrade.SECURE
        securityType.contains("WPA2") -> SecurityGrade.GOOD
        securityType.contains("WPA") -> SecurityGrade.FAIR
        securityType.contains("WEP") -> SecurityGrade.POOR
        securityType.isEmpty() || securityType == "OPEN" -> SecurityGrade.OPEN
        else -> SecurityGrade.FAIR
    }
}

enum class SecurityGrade(val label: String) {
    SECURE("WPA3"),
    GOOD("WPA2"),
    FAIR("WPA"),
    POOR("WEP"),
    OPEN("OPEN")
}

private fun frequencyToChannel(freq: Int): Int = when {
    freq == 2412 -> 1; freq == 2417 -> 2; freq == 2422 -> 3
    freq == 2427 -> 4; freq == 2432 -> 5; freq == 2437 -> 6
    freq == 2442 -> 7; freq == 2447 -> 8; freq == 2452 -> 9
    freq == 2457 -> 10; freq == 2462 -> 11; freq == 2467 -> 12
    freq == 2472 -> 13; freq == 2484 -> 14
    freq in 5000..5900 -> (freq - 5000) / 5
    else -> 0
}
