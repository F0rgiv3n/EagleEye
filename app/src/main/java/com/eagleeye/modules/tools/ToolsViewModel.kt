package com.eagleeye.modules.tools

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.*
import com.eagleeye.modules.cve.CveRepository
import com.eagleeye.modules.export.ReportExporter
import com.eagleeye.modules.portal.CaptivePortalDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    val tools = NetworkTools(application)
    private val sslInspector = SslInspector()
    private val vpnDetector = VpnLeakDetector(application)
    private val cveRepo = CveRepository()
    private val exporter = ReportExporter(application)
    private val portalDetector = CaptivePortalDetector()

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

    // ── SSL Inspector ──
    private val _sslResult = MutableStateFlow<SslCertInfo?>(null)
    val sslResult: StateFlow<SslCertInfo?> = _sslResult.asStateFlow()
    private val _sslRunning = MutableStateFlow(false)
    val sslRunning: StateFlow<Boolean> = _sslRunning.asStateFlow()

    fun runSslInspect(host: String, port: Int = 443) {
        viewModelScope.launch(Dispatchers.IO) {
            _sslRunning.value = true
            _sslResult.value = null
            _sslResult.value = sslInspector.inspect(host, port)
            _sslRunning.value = false
        }
    }

    // ── VPN Leak ──
    private val _vpnLeakResult = MutableStateFlow<VpnLeakResult?>(null)
    val vpnLeakResult: StateFlow<VpnLeakResult?> = _vpnLeakResult.asStateFlow()
    private val _vpnRunning = MutableStateFlow(false)
    val vpnRunning: StateFlow<Boolean> = _vpnRunning.asStateFlow()

    fun runVpnLeakTest() {
        viewModelScope.launch(Dispatchers.IO) {
            _vpnRunning.value = true
            _vpnLeakResult.value = null
            _vpnLeakResult.value = vpnDetector.detect()
            _vpnRunning.value = false
        }
    }

    // ── CVE Lookup ──
    private val _cveResult = MutableStateFlow<CveLookupResult?>(null)
    val cveResult: StateFlow<CveLookupResult?> = _cveResult.asStateFlow()
    private val _cveRunning = MutableStateFlow(false)
    val cveRunning: StateFlow<Boolean> = _cveRunning.asStateFlow()

    fun searchCves(keyword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _cveRunning.value = true
            _cveResult.value = null
            _cveResult.value = cveRepo.searchCves(keyword)
            _cveRunning.value = false
        }
    }

    // ── Export ──
    private val _exportIntent = MutableStateFlow<Intent?>(null)
    val exportIntent: StateFlow<Intent?> = _exportIntent.asStateFlow()

    fun exportReport(
        wifi: WifiConnectionInfo,
        score: SecurityScore?,
        devices: List<LanDevice>,
        asJson: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = if (asJson)
                exporter.exportJson(wifi, score, devices)
            else
                exporter.exportText(wifi, score, devices)
            _exportIntent.value = intent
        }
    }
    fun clearExportIntent() { _exportIntent.value = null }

    fun getServiceName(port: Int) = tools.getServiceName(port)

    // ── Captive Portal ──
    private val _portalResult = MutableStateFlow<CaptivePortalResult?>(null)
    val portalResult: StateFlow<CaptivePortalResult?> = _portalResult.asStateFlow()
    private val _portalRunning = MutableStateFlow(false)
    val portalRunning: StateFlow<Boolean> = _portalRunning.asStateFlow()

    fun runPortalCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            _portalRunning.value = true
            _portalResult.value = null
            _portalResult.value = portalDetector.detect()
            _portalRunning.value = false
        }
    }
}
