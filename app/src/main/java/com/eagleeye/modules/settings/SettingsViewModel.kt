package com.eagleeye.modules.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setOnboardingDone() = viewModelScope.launch {
        repo.update { copy(onboardingDone = true) }
    }

    fun setScanInterval(minutes: Int) = viewModelScope.launch {
        repo.update { copy(scanIntervalMinutes = minutes) }
    }

    fun setNotify(key: String, value: Boolean) = viewModelScope.launch {
        repo.update {
            when (key) {
                "newDevice" -> copy(notifyNewDevice = value)
                "arp"       -> copy(notifyArpSpoof = value)
                "evilTwin"  -> copy(notifyEvilTwin = value)
                "dns"       -> copy(notifyDnsChange = value)
                "weakSec"   -> copy(notifyWeakSecurity = value)
                else        -> this
            }
        }
    }

    fun setPortScanQuick(quick: Boolean) = viewModelScope.launch {
        repo.update { copy(portScanQuickMode = quick) }
    }

    fun setAutoStartMonitor(enabled: Boolean) = viewModelScope.launch {
        repo.update { copy(autoStartMonitor = enabled) }
    }
}
