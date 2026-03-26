package com.eagleeye.data

enum class ThreatLevel { CRITICAL, HIGH, MEDIUM, LOW, INFO }

data class Threat(
    val id: String,
    val level: ThreatLevel,
    val title: String,
    val description: String,
    val recommendation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

data class SecurityScore(
    val total: Int,           // 0-100
    val encryption: Int,      // 0-30
    val noWps: Int,           // 0-15
    val noEvilTwin: Int,      // 0-15
    val dnsIntegrity: Int,    // 0-10
    val noUnknownDevices: Int,// 0-15
    val noOpenPorts: Int,     // 0-15
    val threats: List<Threat>
) {
    val grade: String get() = when {
        total >= 90 -> "A+"
        total >= 80 -> "A"
        total >= 70 -> "B"
        total >= 60 -> "C"
        total >= 40 -> "D"
        else -> "F"
    }

    val gradeColor: String get() = when {
        total >= 80 -> "GREEN"
        total >= 60 -> "YELLOW"
        total >= 40 -> "ORANGE"
        else -> "RED"
    }
}

data class ArpEntry(
    val ip: String,
    val mac: String,
    val timestamp: Long = System.currentTimeMillis()
)
