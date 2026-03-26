package com.eagleeye.modules.iot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.IoTProfile
import com.eagleeye.data.LanDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IoTViewModel(application: Application) : AndroidViewModel(application) {

    private val profiler = IoTProfiler()

    private val _profiles = MutableStateFlow<Map<String, IoTProfile>>(emptyMap())
    val profiles: StateFlow<Map<String, IoTProfile>> = _profiles.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    fun profileDevices(devices: List<LanDevice>) {
        if (_scanning.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _scanning.value = true
            val ssdp = SsdpScanner.scan()
            val result = mutableMapOf<String, IoTProfile>()
            devices.forEach { device ->
                val profile = profiler.profile(device, ssdp)
                result[device.ip] = profile
            }
            _profiles.value = result
            _scanning.value = false
        }
    }
}
