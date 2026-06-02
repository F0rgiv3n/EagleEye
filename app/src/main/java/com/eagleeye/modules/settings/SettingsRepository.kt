package com.eagleeye.modules.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.eagleeye.data.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "eagleeye_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SCAN_INTERVAL_MINUTES = intPreferencesKey("scan_interval_minutes")
        val NOTIFY_NEW_DEVICE = booleanPreferencesKey("notify_new_device")
        val NOTIFY_ARP = booleanPreferencesKey("notify_arp")
        val NOTIFY_EVIL_TWIN = booleanPreferencesKey("notify_evil_twin")
        val NOTIFY_DNS = booleanPreferencesKey("notify_dns")
        val NOTIFY_WEAK_SEC = booleanPreferencesKey("notify_weak_sec")
        val PORT_SCAN_QUICK = booleanPreferencesKey("port_scan_quick")
        val AUTO_START_MONITOR = booleanPreferencesKey("auto_start_monitor")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SHODAN_API_KEY = stringPreferencesKey("shodan_api_key")
        val ABUSE_IPDB_KEY = stringPreferencesKey("abuse_ipdb_key")
        val AUTO_SCAN_ON_CONNECT = booleanPreferencesKey("auto_scan_on_connect")
        val TRUSTED_SSIDS = stringSetPreferencesKey("trusted_ssids")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            scanIntervalMinutes = prefs[Keys.SCAN_INTERVAL_MINUTES] ?: 15,
            notifyNewDevice = prefs[Keys.NOTIFY_NEW_DEVICE] ?: true,
            notifyArpSpoof = prefs[Keys.NOTIFY_ARP] ?: true,
            notifyEvilTwin = prefs[Keys.NOTIFY_EVIL_TWIN] ?: true,
            notifyDnsChange = prefs[Keys.NOTIFY_DNS] ?: true,
            notifyWeakSecurity = prefs[Keys.NOTIFY_WEAK_SEC] ?: false,
            portScanQuickMode = prefs[Keys.PORT_SCAN_QUICK] ?: true,
            autoStartMonitor = prefs[Keys.AUTO_START_MONITOR] ?: false,
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            shodanApiKey = prefs[Keys.SHODAN_API_KEY] ?: "",
            abuseIpDbKey = prefs[Keys.ABUSE_IPDB_KEY] ?: "",
            autoScanOnConnect = prefs[Keys.AUTO_SCAN_ON_CONNECT] ?: false,
            trustedSsids = prefs[Keys.TRUSTED_SSIDS] ?: emptySet(),
            demoMode = prefs[Keys.DEMO_MODE] ?: false
        )
    }

    suspend fun update(transform: AppSettings.() -> AppSettings) {
        val current = settings.first()
        val updated = current.transform()
        context.dataStore.edit { prefs ->
            prefs[Keys.SCAN_INTERVAL_MINUTES] = updated.scanIntervalMinutes
            prefs[Keys.NOTIFY_NEW_DEVICE] = updated.notifyNewDevice
            prefs[Keys.NOTIFY_ARP] = updated.notifyArpSpoof
            prefs[Keys.NOTIFY_EVIL_TWIN] = updated.notifyEvilTwin
            prefs[Keys.NOTIFY_DNS] = updated.notifyDnsChange
            prefs[Keys.NOTIFY_WEAK_SEC] = updated.notifyWeakSecurity
            prefs[Keys.PORT_SCAN_QUICK] = updated.portScanQuickMode
            prefs[Keys.AUTO_START_MONITOR] = updated.autoStartMonitor
            prefs[Keys.ONBOARDING_DONE] = updated.onboardingDone
            prefs[Keys.SHODAN_API_KEY] = updated.shodanApiKey
            prefs[Keys.ABUSE_IPDB_KEY] = updated.abuseIpDbKey
            prefs[Keys.AUTO_SCAN_ON_CONNECT] = updated.autoScanOnConnect
            prefs[Keys.TRUSTED_SSIDS] = updated.trustedSsids
            prefs[Keys.DEMO_MODE] = updated.demoMode
        }
    }
}
