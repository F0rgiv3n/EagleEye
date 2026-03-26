package com.eagleeye.modules.monitor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.eagleeye.data.EventSeverity
import com.eagleeye.data.EventType
import com.eagleeye.data.MonitorConfig
import com.eagleeye.data.NetworkEvent
import com.eagleeye.data.db.AppDatabase
import kotlinx.coroutines.*

class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var engine: MonitorEngine
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        engine = MonitorEngine(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val intervalMin = intent.getIntExtra(EXTRA_INTERVAL, 15)
                val config = MonitorConfig(
                    isEnabled = true,
                    intervalMinutes = intervalMin,
                    notifyNewDevice  = intent.getBooleanExtra(EXTRA_NEW_DEVICE, true),
                    notifyArpSpoof   = intent.getBooleanExtra(EXTRA_ARP_SPOOF, true),
                    notifyEvilTwin   = intent.getBooleanExtra(EXTRA_EVIL_TWIN, true),
                    notifyDnsChange  = intent.getBooleanExtra(EXTRA_DNS_CHANGE, true),
                    notifyWeakSecurity = intent.getBooleanExtra(EXTRA_WEAK_SEC, false)
                )
                startMonitor(config)
            }
            ACTION_STOP -> stopMonitor()
        }
        return START_STICKY
    }

    private fun startMonitor(config: MonitorConfig) {
        val notification = NotificationHelper.buildForegroundNotification(
            this, "Monitoring network — every ${config.intervalMinutes} min"
        )
        startForeground(NotificationHelper.FOREGROUND_ID, notification)

        // Log start event
        scope.launch {
            AppDatabase.getInstance(this@MonitorService).networkEventDao().insert(
                NetworkEvent(
                    type = EventType.MONITOR_STARTED,
                    severity = EventSeverity.INFO,
                    title = "Monitor Started",
                    detail = "Background monitoring active. Scanning every ${config.intervalMinutes} minute(s)."
                )
            )
        }

        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                runCycle(config)
                delay(config.intervalMinutes * 60_000L)
            }
        }
    }

    private suspend fun runCycle(config: MonitorConfig) {
        try {
            val events = engine.runCycle(config)
            // Notify for non-INFO events
            val alertable = events.filter {
                it.type != EventType.SCAN_COMPLETE && it.severity != EventSeverity.INFO
            }
            alertable.forEach { event ->
                NotificationHelper.sendThreatNotification(this, event)
            }
            // Update foreground notification with scan summary
            val threatCount = alertable.size
            val statusText = if (threatCount == 0)
                "Last scan: no threats detected"
            else
                "⚠ $threatCount threat(s) detected — tap to view"
            updateForegroundNotification(statusText)
        } catch (e: Exception) {
            // Don't crash the service on scan errors
        }
    }

    private fun updateForegroundNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(
            NotificationHelper.FOREGROUND_ID,
            NotificationHelper.buildForegroundNotification(this, text)
        )
    }

    private fun stopMonitor() {
        monitorJob?.cancel()
        scope.launch {
            AppDatabase.getInstance(this@MonitorService).networkEventDao().insert(
                NetworkEvent(
                    type = EventType.MONITOR_STOPPED,
                    severity = EventSeverity.INFO,
                    title = "Monitor Stopped",
                    detail = "Background monitoring has been disabled."
                )
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START    = "com.eagleeye.MONITOR_START"
        const val ACTION_STOP     = "com.eagleeye.MONITOR_STOP"
        const val EXTRA_INTERVAL  = "interval_minutes"
        const val EXTRA_NEW_DEVICE = "notify_new_device"
        const val EXTRA_ARP_SPOOF  = "notify_arp_spoof"
        const val EXTRA_EVIL_TWIN  = "notify_evil_twin"
        const val EXTRA_DNS_CHANGE = "notify_dns_change"
        const val EXTRA_WEAK_SEC   = "notify_weak_security"

        fun buildStartIntent(context: Context, config: MonitorConfig) =
            Intent(context, MonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTERVAL,   config.intervalMinutes)
                putExtra(EXTRA_NEW_DEVICE, config.notifyNewDevice)
                putExtra(EXTRA_ARP_SPOOF,  config.notifyArpSpoof)
                putExtra(EXTRA_EVIL_TWIN,  config.notifyEvilTwin)
                putExtra(EXTRA_DNS_CHANGE, config.notifyDnsChange)
                putExtra(EXTRA_WEAK_SEC,   config.notifyWeakSecurity)
            }

        fun buildStopIntent(context: Context) =
            Intent(context, MonitorService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
