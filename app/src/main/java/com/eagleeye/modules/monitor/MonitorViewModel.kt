package com.eagleeye.modules.monitor

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.MonitorConfig
import com.eagleeye.data.NetworkEvent
import com.eagleeye.data.db.AppDatabase
import com.eagleeye.modules.settings.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).networkEventDao()
    private val settingsRepo = SettingsRepository(getApplication())

    val events: StateFlow<List<NetworkEvent>> = dao.observeRecent(100)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unreadCount: StateFlow<Int> = dao.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _config = MutableStateFlow(MonitorConfig())
    val config: StateFlow<MonitorConfig> = _config.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { s ->
                _config.value = _config.value.copy(
                    intervalMinutes = s.scanIntervalMinutes,
                    notifyNewDevice = s.notifyNewDevice,
                    notifyArpSpoof = s.notifyArpSpoof,
                    notifyEvilTwin = s.notifyEvilTwin,
                    notifyDnsChange = s.notifyDnsChange,
                    notifyWeakSecurity = s.notifyWeakSecurity
                )
            }
        }
    }

    fun startMonitor(context: Context) {
        val cfg = _config.value.copy(isEnabled = true)
        _config.value = cfg
        _isRunning.value = true
        context.startForegroundService(MonitorService.buildStartIntent(context, cfg))
    }

    fun stopMonitor(context: Context) {
        _isRunning.value = false
        _config.value = _config.value.copy(isEnabled = false)
        context.startService(MonitorService.buildStopIntent(context))
    }

    fun updateConfig(config: MonitorConfig) {
        _config.value = config
        viewModelScope.launch {
            settingsRepo.update {
                copy(
                    scanIntervalMinutes = config.intervalMinutes,
                    notifyNewDevice = config.notifyNewDevice,
                    notifyArpSpoof = config.notifyArpSpoof,
                    notifyEvilTwin = config.notifyEvilTwin,
                    notifyDnsChange = config.notifyDnsChange,
                    notifyWeakSecurity = config.notifyWeakSecurity
                )
            }
        }
    }

    fun markAllRead() = viewModelScope.launch { dao.markAllRead() }
    fun clearAll() = viewModelScope.launch { dao.deleteAll() }
}
