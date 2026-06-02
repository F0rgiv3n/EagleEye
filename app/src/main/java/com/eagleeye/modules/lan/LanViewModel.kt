package com.eagleeye.modules.lan

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.DemoOverrides
import com.eagleeye.data.LanDevice
import com.eagleeye.data.ScanSnapshot
import com.eagleeye.modules.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val progress: Float) : ScanState()
    data class Done(val devices: List<LanDevice>) : ScanState()
    data class Error(val message: String) : ScanState()
}

class LanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LanRepository(application)
    private val settingsRepo = SettingsRepository(application)

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val savedDevices: StateFlow<List<LanDevice>> =
        combine(repository.savedDevices, settingsRepo.settings) { real, settings ->
            if (settings.demoMode) DemoOverrides.lanDevices else real
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Scan History (in-session, last 5 snapshots) ───────────────────────────
    private val _scanHistory = MutableStateFlow<List<ScanSnapshot>>(emptyList())
    val scanHistory: StateFlow<List<ScanSnapshot>> = _scanHistory.asStateFlow()

    private val connectivityManager =
        application.getSystemService(ConnectivityManager::class.java)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                viewModelScope.launch {
                    val autoScan = settingsRepo.settings.firstOrNull()?.autoScanOnConnect ?: false
                    if (autoScan && _scanState.value !is ScanState.Scanning) {
                        startScan()
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.scanProgress.collect { progress ->
                if (_scanState.value is ScanState.Scanning) {
                    _scanState.value = ScanState.Scanning(progress)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) {}
    }

    fun startScan() {
        viewModelScope.launch(Dispatchers.IO) {
            val previousIps = when (val s = _scanState.value) {
                is ScanState.Done -> s.devices.map { it.ip }.toSet()
                else -> savedDevices.value.map { it.ip }.toSet()
            }

            _scanState.value = ScanState.Scanning(0f)
            try {
                val devices = repository.scanNetwork()
                _scanState.value = ScanState.Done(devices)

                val snapshot = ScanSnapshot(
                    timestamp = System.currentTimeMillis(),
                    totalDevices = devices.size,
                    onlineDevices = devices.count { it.isOnline },
                    newDevices = devices.count { it.ip !in previousIps },
                    deviceIps = devices.map { it.ip }.toSet()
                )
                _scanHistory.value = (_scanHistory.value + snapshot).takeLast(5)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun markKnown(mac: String, known: Boolean) {
        viewModelScope.launch { repository.markDeviceKnown(mac, known) }
    }

    fun setAlias(mac: String, alias: String) {
        viewModelScope.launch { repository.setDeviceAlias(mac, alias) }
    }

    fun getServiceName(port: Int) = repository.getServiceName(port)
}
