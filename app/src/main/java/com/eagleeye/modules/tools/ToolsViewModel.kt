package com.eagleeye.modules.tools

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    val tools = NetworkTools(application)

    // ── Ping ──
    private val _pingResult = MutableStateFlow<PingResult?>(null)
    val pingResult: StateFlow<PingResult?> = _pingResult.asStateFlow()

    private val _pingRunning = MutableStateFlow(false)
    val pingRunning: StateFlow<Boolean> = _pingRunning.asStateFlow()

    fun runPing(host: String, count: Int = 8) {
        viewModelScope.launch(Dispatchers.IO) {
            _pingRunning.value = true
            _pingResult.value = null
            _pingResult.value = tools.ping(host, count)
            _pingRunning.value = false
        }
    }

    // ── Traceroute ──
    private val _traceHops = MutableStateFlow<List<TracerouteHop>>(emptyList())
    val traceHops: StateFlow<List<TracerouteHop>> = _traceHops.asStateFlow()

    private val _traceRunning = MutableStateFlow(false)
    val traceRunning: StateFlow<Boolean> = _traceRunning.asStateFlow()

    fun runTraceroute(host: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _traceRunning.value = true
            _traceHops.value = emptyList()
            _traceHops.value = tools.traceroute(host)
            _traceRunning.value = false
        }
    }

    // ── Port Scan ──
    private val _portResults = MutableStateFlow<List<PortScanResult>>(emptyList())
    val portResults: StateFlow<List<PortScanResult>> = _portResults.asStateFlow()

    private val _portScanProgress = MutableStateFlow(0f)
    val portScanProgress: StateFlow<Float> = _portScanProgress.asStateFlow()

    private val _portScanRunning = MutableStateFlow(false)
    val portScanRunning: StateFlow<Boolean> = _portScanRunning.asStateFlow()

    fun runPortScan(host: String, quick: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _portScanRunning.value = true
            _portResults.value = emptyList()
            _portScanProgress.value = 0f
            val ports = if (quick) NetworkTools.QUICK_PORTS else NetworkTools.TOP_PORTS
            val results = tools.scanPorts(host, ports) { done ->
                _portScanProgress.value = done.toFloat() / ports.size
            }
            _portResults.value = results
            _portScanRunning.value = false
        }
    }

    // ── DNS ──
    private val _dnsResult = MutableStateFlow<DnsResult?>(null)
    val dnsResult: StateFlow<DnsResult?> = _dnsResult.asStateFlow()

    private val _dnsRunning = MutableStateFlow(false)
    val dnsRunning: StateFlow<Boolean> = _dnsRunning.asStateFlow()

    fun runDnsLookup(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _dnsRunning.value = true
            _dnsResult.value = null
            _dnsResult.value = tools.dnsLookup(query)
            _dnsRunning.value = false
        }
    }

    // ── Public IP ──
    private val _publicIp = MutableStateFlow<PublicIpInfo?>(null)
    val publicIp: StateFlow<PublicIpInfo?> = _publicIp.asStateFlow()

    private val _ipLoading = MutableStateFlow(false)
    val ipLoading: StateFlow<Boolean> = _ipLoading.asStateFlow()

    fun fetchPublicIp() {
        viewModelScope.launch(Dispatchers.IO) {
            _ipLoading.value = true
            _publicIp.value = tools.getPublicIpInfo()
            _ipLoading.value = false
        }
    }

    // ── Wake on LAN ──
    private val _wolStatus = MutableStateFlow<String?>(null)
    val wolStatus: StateFlow<String?> = _wolStatus.asStateFlow()

    fun sendWakeOnLan(mac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = tools.wakeOnLan(mac)
            _wolStatus.value = if (ok) "Magic packet sent to $mac" else "Failed — check MAC format"
        }
    }

    fun clearWolStatus() { _wolStatus.value = null }
}
