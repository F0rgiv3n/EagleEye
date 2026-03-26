package com.eagleeye.modules.wifi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.ScannedNetwork
import com.eagleeye.data.SignalSample
import com.eagleeye.data.WifiConnectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WifiRepository(application)

    private val _connectionInfo = MutableStateFlow(WifiConnectionInfo())
    val connectionInfo: StateFlow<WifiConnectionInfo> = _connectionInfo.asStateFlow()

    private val _scanResults = MutableStateFlow<List<ScannedNetwork>>(emptyList())
    val scanResults: StateFlow<List<ScannedNetwork>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _signalHistory = MutableStateFlow<List<SignalSample>>(emptyList())
    val signalHistory: StateFlow<List<SignalSample>> = _signalHistory.asStateFlow()

    init {
        startObservingConnection()
    }

    private fun startObservingConnection() {
        viewModelScope.launch {
            repository.observeConnectionInfo()
                .distinctUntilChanged()
                .collect { info ->
                    _connectionInfo.value = info
                    if (info.isConnected && info.rssi != 0) {
                        _signalHistory.value = (_signalHistory.value +
                            SignalSample(rssi = info.rssi, ssid = info.ssid)).takeLast(60)
                    }
                }
        }
    }

    fun startNetworkScan() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            val results = repository.getScanResults()
            _scanResults.value = results
            _isScanning.value = false
        }
    }
}
