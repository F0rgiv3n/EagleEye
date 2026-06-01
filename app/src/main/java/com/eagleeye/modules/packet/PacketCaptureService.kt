package com.eagleeye.modules.packet

import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.ServiceCompat
import com.eagleeye.data.CapturedPacket
import com.eagleeye.modules.monitor.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.FileInputStream
import java.io.FileOutputStream

class PacketCaptureService : VpnService() {

    companion object {
        const val ACTION_START = "com.eagleeye.packet.START"
        const val ACTION_STOP  = "com.eagleeye.packet.STOP"

        val packetFlow = MutableSharedFlow<CapturedPacket>(
            extraBufferCapacity = 500,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        var isRunning = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> { startCapture(); START_STICKY }
            ACTION_STOP  -> { stopCapture(); stopSelf(); START_NOT_STICKY }
            else -> START_NOT_STICKY
        }
    }

    private fun startCapture() {
        if (isRunning) return

        // Must call startForeground before establishing VPN on Android 12+,
        // otherwise the system kills the service or throws FGS-not-allowed.
        val notification = NotificationHelper.buildPacketCaptureNotification(this)
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        ServiceCompat.startForeground(this, NotificationHelper.PACKET_FG_ID, notification, fgType)

        val builder = Builder()
            .setSession("EagleEye Packet Analyzer")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .setMtu(1500)
            .setBlocking(true)
        vpnInterface = builder.establish() ?: run {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            return
        }
        isRunning = true

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { serviceScope = it }
        scope.launch {
            val fd = vpnInterface?.fileDescriptor ?: return@launch
            val inputStream = FileInputStream(fd)
            @Suppress("UNUSED_VARIABLE")
            val outputStream = FileOutputStream(fd)
            val buf = ByteArray(32767)

            while (isRunning) {
                val len = try { inputStream.read(buf) } catch (_: Exception) { break }
                if (len <= 0) continue
                PacketParser.parse(buf, len)?.let { packet ->
                    packetFlow.tryEmit(packet)
                }
                // Analysis-only mode: packets are not forwarded.
                // Internet access is paused while capture is active.
            }
        }
    }

    private fun stopCapture() {
        isRunning = false
        serviceScope?.cancel()
        serviceScope = null
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
