package com.eagleeye.modules.packet

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.CapturedPacket
import com.eagleeye.data.IpProtocol
import com.eagleeye.data.PacketStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PacketViewModel(app: Application) : AndroidViewModel(app) {

    private val _isCapturing = MutableStateFlow(PacketCaptureService.isRunning)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _recentPackets = MutableStateFlow<List<CapturedPacket>>(emptyList())
    val recentPackets: StateFlow<List<CapturedPacket>> = _recentPackets.asStateFlow()

    private val _stats = MutableStateFlow(PacketStats())
    val stats: StateFlow<PacketStats> = _stats.asStateFlow()

    private val _vpnPermissionIntent = MutableStateFlow<Intent?>(null)
    val vpnPermissionIntent: StateFlow<Intent?> = _vpnPermissionIntent.asStateFlow()

    private var totalPackets = 0
    private var totalBytes = 0L
    private var tcpCount = 0
    private var udpCount = 0
    private var icmpCount = 0
    private val dnsQuerySet = LinkedHashSet<String>()
    private val destCounts = HashMap<String, Int>()

    private fun isPrivateIp(ip: String): Boolean {
        return ip.startsWith("192.168.") ||
               ip.startsWith("10.") ||
               ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
               ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
               ip.startsWith("172.20.") || ip.startsWith("172.21.") ||
               ip.startsWith("172.22.") || ip.startsWith("172.23.") ||
               ip.startsWith("172.24.") || ip.startsWith("172.25.") ||
               ip.startsWith("172.26.") || ip.startsWith("172.27.") ||
               ip.startsWith("172.28.") || ip.startsWith("172.29.") ||
               ip.startsWith("172.30.") || ip.startsWith("172.31.")
    }

    init {
        viewModelScope.launch {
            PacketCaptureService.packetFlow.collect { packet ->
                _isCapturing.value = PacketCaptureService.isRunning

                val updated = (_recentPackets.value + packet).takeLast(200)
                _recentPackets.value = updated

                totalPackets++
                totalBytes += packet.size
                when (packet.protocol) {
                    IpProtocol.TCP  -> tcpCount++
                    IpProtocol.UDP  -> udpCount++
                    IpProtocol.ICMP -> icmpCount++
                    else -> {}
                }
                if (packet.dnsQuery.isNotEmpty()) {
                    dnsQuerySet.add(packet.dnsQuery)
                }
                if (!isPrivateIp(packet.dstIp)) {
                    destCounts[packet.dstIp] = (destCounts[packet.dstIp] ?: 0) + 1
                }

                val topDest = destCounts.entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .map { it.key to it.value }

                _stats.value = PacketStats(
                    totalPackets = totalPackets,
                    totalBytes = totalBytes,
                    tcpPackets = tcpCount,
                    udpPackets = udpCount,
                    icmpPackets = icmpCount,
                    dnsQueries = dnsQuerySet.toList(),
                    topDestinations = topDest
                )
            }
        }
    }

    fun startCapture(context: Context) {
        val permIntent = VpnService.prepare(context)
        if (permIntent != null) {
            _vpnPermissionIntent.value = permIntent
        } else {
            launchService(context)
        }
    }

    fun stopCapture(context: Context) {
        context.startService(
            Intent(context, PacketCaptureService::class.java).apply {
                action = PacketCaptureService.ACTION_STOP
            }
        )
        _isCapturing.value = false
    }

    fun onVpnPermissionResult(granted: Boolean, context: Context) {
        if (granted) launchService(context)
    }

    fun clearVpnPermissionIntent() {
        _vpnPermissionIntent.value = null
    }

    fun resetStats() {
        totalPackets = 0
        totalBytes = 0L
        tcpCount = 0
        udpCount = 0
        icmpCount = 0
        dnsQuerySet.clear()
        destCounts.clear()
        _recentPackets.value = emptyList()
        _stats.value = PacketStats()
    }

    private fun launchService(context: Context) {
        context.startService(
            Intent(context, PacketCaptureService::class.java).apply {
                action = PacketCaptureService.ACTION_START
            }
        )
        _isCapturing.value = true
    }
}
