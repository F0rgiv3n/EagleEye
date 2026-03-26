package com.eagleeye.modules.mac

import android.app.Application
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.MacInfo
import com.eagleeye.data.MacProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MacViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MacRepository(application)

    private val _macInfo = MutableStateFlow<MacInfo?>(null)
    val macInfo: StateFlow<MacInfo?> = _macInfo.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    private val _newMacPreview = MutableStateFlow("")
    val newMacPreview: StateFlow<String> = _newMacPreview.asStateFlow()

    val profiles: StateFlow<List<MacProfile>> = repository.profiles
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @Suppress("DEPRECATION")
    val currentSsid: String
        get() = (getApplication<Application>()
            .getSystemService(android.content.Context.WIFI_SERVICE) as WifiManager)
            .connectionInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: ""

    init { refreshMacInfo() }

    fun refreshMacInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _macInfo.value = repository.getMacInfo()
            _loading.value = false
        }
    }

    fun generateRandomMac() {
        _newMacPreview.value = repository.generateRandomMac()
    }

    fun setNewMacPreview(mac: String) {
        _newMacPreview.value = mac
    }

    fun applyMac(mac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            val result = repository.changeMac(mac)
            _actionResult.value = when (result) {
                MacChangeResult.Success -> "MAC changed to $mac"
                MacChangeResult.RootDenied -> "Root access required to change MAC"
                MacChangeResult.InvalidFormat -> "Invalid MAC format (use AA:BB:CC:DD:EE:FF)"
                is MacChangeResult.Error -> "Error: ${result.message}"
            }
            if (result == MacChangeResult.Success) {
                _macInfo.value = repository.getMacInfo()
                _newMacPreview.value = ""
            }
            _loading.value = false
        }
    }

    fun resetToRandom() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            val result = repository.resetToRandom()
            _actionResult.value = when (result) {
                MacChangeResult.Success -> "MAC randomized successfully"
                MacChangeResult.RootDenied -> "Root access required"
                else -> "Failed to randomize MAC"
            }
            if (result == MacChangeResult.Success) {
                _macInfo.value = repository.getMacInfo()
            }
            _loading.value = false
        }
    }

    fun saveProfile(ssid: String, mac: String, autoRotate: Boolean, intervalHours: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProfile(
                MacProfile(
                    ssid = ssid,
                    mac = mac,
                    isAutoRotate = autoRotate,
                    rotateIntervalHours = intervalHours,
                    lastRotated = System.currentTimeMillis()
                )
            )
            _actionResult.value = "Profile saved for \"$ssid\""
        }
    }

    fun deleteProfile(profile: MacProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProfile(profile)
        }
    }

    fun applyProfileForCurrentNetwork() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.applyProfileForCurrentNetwork()
            _actionResult.value = when (result) {
                MacChangeResult.Success -> "Profile MAC applied"
                MacChangeResult.RootDenied -> "Root access required"
                is MacChangeResult.Error -> result.message
                else -> "Failed"
            }
            if (result == MacChangeResult.Success) {
                _macInfo.value = repository.getMacInfo()
            }
        }
    }

    fun clearResult() { _actionResult.value = null }
}
