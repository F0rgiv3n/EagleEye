package com.eagleeye.data

data class AppSettings(
    val scanIntervalMinutes: Int = 15,
    val notifyNewDevice: Boolean = true,
    val notifyArpSpoof: Boolean = true,
    val notifyEvilTwin: Boolean = true,
    val notifyDnsChange: Boolean = true,
    val notifyWeakSecurity: Boolean = false,
    val portScanQuickMode: Boolean = true,
    val autoStartMonitor: Boolean = false,
    val onboardingDone: Boolean = false
)
