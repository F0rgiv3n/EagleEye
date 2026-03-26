package com.eagleeye.modules.lan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.LanDevice
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

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val savedDevices: StateFlow<List<LanDevice>> = repository.savedDevices
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Observe scan progress and forward to state
        viewModelScope.launch {
            repository.scanProgress.collect { progress ->
                if (_scanState.value is ScanState.Scanning) {
                    _scanState.value = ScanState.Scanning(progress)
                }
            }
        }
    }

    fun startScan() {
        viewModelScope.launch(Dispatchers.IO) {
            _scanState.value = ScanState.Scanning(0f)
            try {
                val devices = repository.scanNetwork()
                _scanState.value = ScanState.Done(devices)
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
