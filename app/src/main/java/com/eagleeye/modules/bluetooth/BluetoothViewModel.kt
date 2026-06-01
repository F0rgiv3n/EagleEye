package com.eagleeye.modules.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.BtDevice
import com.eagleeye.data.BtDeviceType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val adapter: BluetoothAdapter? =
        (application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _devices = MutableStateFlow<List<BtDevice>>(emptyList())
    val devices: StateFlow<List<BtDevice>> = _devices.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val found = mutableMapOf<String, BtDevice>()
    private var bleScanner: BluetoothLeScanner? = null
    private var scanJob: Job? = null

    private val classicReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != BluetoothDevice.ACTION_FOUND) return
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
            device?.let { addDevice(it, rssi, isBle = false) }
        }
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val txPower = result.txPower
            addDevice(result.device, result.rssi, isBle = true, txPower = txPower,
                manufacturerData = result.scanRecord?.manufacturerSpecificData)
        }
    }

    // Permission-gated via hasBleScanPermission() and try/catch SecurityException;
    // lint can't trace the helper, so the annotation just silences the warning.
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_scanning.value) return
        if (adapter == null || !adapter.isEnabled) {
            _scanError.value = "Bluetooth is disabled"
            return
        }
        _scanError.value = null
        found.clear()
        _devices.value = emptyList()
        _scanning.value = true

        // Classic BT
        try {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            getApplication<Application>().registerReceiver(classicReceiver, filter)
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            _scanError.value = "Bluetooth permission denied"
            _scanning.value = false
            return
        }

        // BLE — needs BLUETOOTH_SCAN on API 31+; pre-31 it falls under BLUETOOTH_ADMIN
        // which was granted at install time (no runtime check needed).
        bleScanner = adapter.bluetoothLeScanner
        if (hasBleScanPermission()) {
            try {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                bleScanner?.startScan(null, settings, bleScanCallback)
            } catch (e: Exception) { /* BLE scan failed — Classic still runs */ }
        }

        // Auto-stop after 15 seconds
        scanJob = viewModelScope.launch {
            delay(15_000)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        _scanning.value = false
        scanJob?.cancel()
        try { adapter?.cancelDiscovery() } catch (_: SecurityException) {}
        if (hasBleScanPermission()) {
            try { bleScanner?.stopScan(bleScanCallback) } catch (_: Exception) {}
        }
        try { getApplication<Application>().unregisterReceiver(classicReceiver) } catch (_: Exception) {}
    }

    private fun hasBleScanPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun addDevice(
        device: BluetoothDevice,
        rssi: Int,
        isBle: Boolean,
        txPower: Int = Int.MIN_VALUE,
        manufacturerData: android.util.SparseArray<ByteArray>? = null
    ) {
        try {
            val addr = device.address ?: return
            val name = try { device.name ?: "" } catch (_: SecurityException) { "" }
            val btClass = device.bluetoothClass
            val bondState = when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> "BONDED"
                else -> "DISCOVERED"
            }
            val deviceClass = btClass?.let { classToString(it.deviceClass) } ?: ""
            val type = detectType(name, btClass?.deviceClass ?: 0)
            val mfr = manufacturerData?.let { resolveManufacturer(it) } ?: ""

            val btDev = BtDevice(
                address = addr,
                name = name.ifBlank { "Unknown" },
                rssi = rssi,
                deviceType = type,
                deviceClass = deviceClass,
                bondState = bondState,
                isBle = isBle,
                manufacturerName = mfr,
                txPower = txPower
            )
            found[addr] = btDev
            _devices.value = found.values.sortedByDescending { it.rssi }
        } catch (_: SecurityException) {}
    }

    private fun detectType(name: String, devClass: Int): BtDeviceType {
        val n = name.lowercase()
        return when {
            n.contains("airpod") || n.contains("headphone") || n.contains("headset") ||
                n.contains("buds") || n.contains("earphone") || devClass == 1048 -> BtDeviceType.HEADPHONES
            n.contains("speaker") || n.contains("soundbar") || devClass == 1044 -> BtDeviceType.SPEAKER
            n.contains("keyboard") || devClass == 1344 -> BtDeviceType.KEYBOARD
            n.contains("mouse") || devClass == 1408 -> BtDeviceType.MOUSE
            n.contains("watch") || n.contains("band") || n.contains("fit") -> BtDeviceType.WEARABLE
            n.contains("tv") || n.contains("television") || devClass == 1060 -> BtDeviceType.TV
            n.contains("printer") || devClass == 1664 -> BtDeviceType.PRINTER
            n.contains("car") || n.contains("auto") || devClass == 1028 -> BtDeviceType.CAR
            n.contains("camera") || devClass == 1076 -> BtDeviceType.CAMERA
            devClass in 516..532 -> BtDeviceType.PHONE
            devClass in 256..271 -> BtDeviceType.COMPUTER
            devClass in 2304..2336 -> BtDeviceType.HEALTH
            else -> BtDeviceType.UNKNOWN
        }
    }

    private fun classToString(devClass: Int): String = when (devClass) {
        in 256..271  -> "Computer"
        in 516..532  -> "Phone"
        1028         -> "Car Audio"
        1044, 1048   -> "Audio/Video"
        1344         -> "Keyboard"
        1408         -> "Mouse"
        1664         -> "Printer"
        in 2304..2336 -> "Health Device"
        else         -> if (devClass != 0) "Class $devClass" else ""
    }

    private fun resolveManufacturer(data: android.util.SparseArray<ByteArray>): String {
        if (data.size() == 0) return ""
        return when (data.keyAt(0)) {
            0x004C -> "Apple"
            0x0006, 0x0008 -> "Microsoft"
            0x00E0 -> "Google"
            0x0075 -> "Samsung"
            0x0157 -> "Huawei"
            0x0499 -> "Ruuvi"
            0x0059 -> "Nordic Semi"
            else   -> "ID:0x%04X".format(data.keyAt(0))
        }
    }

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }
}
