package com.eagleeye.modules.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.eagleeye.MainActivity
import com.eagleeye.R
import com.eagleeye.data.EventSeverity
import com.eagleeye.data.NetworkEvent

object NotificationHelper {

    const val CHANNEL_THREATS  = "eagleeye_threats"
    const val CHANNEL_MONITOR  = "eagleeye_monitor"
    const val CHANNEL_PACKET   = "eagleeye_packet"
    const val FOREGROUND_ID    = 1001
    const val PACKET_FG_ID     = 1002
    private var notifId        = 2000

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_THREATS,
                "Security Threats",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for ARP spoofing, evil twin, and other threats"
                enableVibration(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                "Background Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification while monitoring is active"
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PACKET,
                "Packet Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification while the packet analyzer is capturing"
            }
        )
    }

    fun buildPacketCaptureNotification(context: Context) =
        NotificationCompat.Builder(context, CHANNEL_PACKET)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("EagleEye Packet Analyzer")
            .setContentText("Capturing traffic — internet is paused")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launchIntent(context))
            .build()

    fun buildForegroundNotification(context: Context, statusText: String) =
        NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle("EagleEye Monitor")
            .setContentText(statusText)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launchIntent(context))
            .build()

    fun sendThreatNotification(context: Context, event: NetworkEvent) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val priority = when (event.severity) {
            EventSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
            EventSeverity.HIGH     -> NotificationCompat.PRIORITY_HIGH
            else                   -> NotificationCompat.PRIORITY_DEFAULT
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_THREATS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(event.title)
            .setContentText(event.detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.detail))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(launchIntent(context))
            .build()

        nm.notify(notifId++, notification)
    }

    private fun launchIntent(context: Context) = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
