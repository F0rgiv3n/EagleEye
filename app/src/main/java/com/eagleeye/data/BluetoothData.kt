package com.eagleeye.data

enum class BtDeviceType {
    PHONE, COMPUTER, HEADPHONES, SPEAKER, KEYBOARD, MOUSE,
    WEARABLE, TV, PRINTER, CAMERA, HEALTH, CAR, UNKNOWN
}

data class BtDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val deviceType: BtDeviceType,
    val deviceClass: String,
    val bondState: String,      // "BONDED" / "DISCOVERED"
    val isBle: Boolean,
    val manufacturerName: String = "",
    val txPower: Int = Int.MIN_VALUE   // BLE only, Int.MIN_VALUE = unknown
)
