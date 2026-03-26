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
    private val headersAnalyzer = HttpHeadersAnalyzer()
    private val threatIntelClient = ThreatIntelClient()
    private val shodanClient = ShodanClient()
    private val whoisClient = WhoisClient()
    private val rogueDhcpDetector = com.eagleeye.modules.security.RogueDhcpDetector(application)

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

    private val eventDao = com.eagleeye.data.db.AppDatabase
        .getInstance(application).networkEventDao()

    fun exportReport(
        wifi: WifiConnectionInfo,
        score: SecurityScore?,
        devices: List<LanDevice>,
        asJson: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val events = eventDao.getRecent(100)
            val intent = if (asJson)
                exporter.exportJson(wifi, score, devices, events)
            else
                exporter.exportText(wifi, score, devices, events)
            _exportIntent.value = intent
        }
    }
    fun clearExportIntent() { _exportIntent.value = null }

    fun getServiceName(port: Int) = tools.getServiceName(port)

    // ── GeoIP Map ──
    private val geoClient = GeoIpClient()
    private val _geoHome = MutableStateFlow<com.eagleeye.data.GeoPoint?>(null)
    val geoHome: StateFlow<com.eagleeye.data.GeoPoint?> = _geoHome.asStateFlow()
    private val _geoPoints = MutableStateFlow<List<com.eagleeye.data.GeoPoint>>(emptyList())
    val geoPoints: StateFlow<List<com.eagleeye.data.GeoPoint>> = _geoPoints.asStateFlow()
    private val _geoRunning = MutableStateFlow(false)
    val geoRunning: StateFlow<Boolean> = _geoRunning.asStateFlow()

    fun loadGeoMap(extraIps: List<String> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            _geoRunning.value = true
            val home = geoClient.resolveHome()
            _geoHome.value = home
            val defaultIps = listOf("8.8.8.8", "1.1.1.1", "9.9.9.9", "208.67.222.222") + extraIps
            val toResolve = defaultIps.distinct().filter { it != home?.ip }
            _geoPoints.value = geoClient.resolveIps(toResolve)
            _geoRunning.value = false
        }
    }

    // ── WHOIS ──
    private val _whoisResult = MutableStateFlow<com.eagleeye.data.WhoisResult?>(null)
    val whoisResult: StateFlow<com.eagleeye.data.WhoisResult?> = _whoisResult.asStateFlow()
    private val _whoisRunning = MutableStateFlow(false)
    val whoisRunning: StateFlow<Boolean> = _whoisRunning.asStateFlow()

    fun runWhois(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _whoisRunning.value = true
            _whoisResult.value = null
            _whoisResult.value = whoisClient.lookup(query)
            _whoisRunning.value = false
        }
    }

    // ── Rogue DHCP ──
    private val _rogueDhcpResult = MutableStateFlow<com.eagleeye.data.RogueDhcpResult?>(null)
    val rogueDhcpResult: StateFlow<com.eagleeye.data.RogueDhcpResult?> = _rogueDhcpResult.asStateFlow()
    private val _rogueDhcpRunning = MutableStateFlow(false)
    val rogueDhcpRunning: StateFlow<Boolean> = _rogueDhcpRunning.asStateFlow()

    fun runRogueDhcpScan() {
        viewModelScope.launch(Dispatchers.IO) {
            _rogueDhcpRunning.value = true
            _rogueDhcpResult.value = null
            _rogueDhcpResult.value = rogueDhcpDetector.detect()
            _rogueDhcpRunning.value = false
        }
    }

    fun addGeoIp(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _geoPoints.value.map { it.ip }.toSet()
            if (ip in existing || ip == _geoHome.value?.ip) return@launch
            val result = geoClient.resolveIps(listOf(ip))
            if (result.isNotEmpty()) {
                _geoPoints.value = _geoPoints.value + result
            }
        }
    }

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

    // ── HTTP Headers ──
    private val _headersResult = MutableStateFlow<com.eagleeye.data.HttpHeadersResult?>(null)
    val headersResult: StateFlow<com.eagleeye.data.HttpHeadersResult?> = _headersResult.asStateFlow()
    private val _headersRunning = MutableStateFlow(false)
    val headersRunning: StateFlow<Boolean> = _headersRunning.asStateFlow()

    fun runHeadersCheck(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _headersRunning.value = true
            _headersResult.value = null
            _headersResult.value = headersAnalyzer.analyze(url)
            _headersRunning.value = false
        }
    }

    // ── Threat Intel ──
    private val _threatResult = MutableStateFlow<com.eagleeye.data.ThreatIntelResult?>(null)
    val threatResult: StateFlow<com.eagleeye.data.ThreatIntelResult?> = _threatResult.asStateFlow()
    private val _threatRunning = MutableStateFlow(false)
    val threatRunning: StateFlow<Boolean> = _threatRunning.asStateFlow()

    fun runThreatIntel(ip: String, abuseKey: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _threatRunning.value = true
            _threatResult.value = null
            _threatResult.value = threatIntelClient.check(ip, abuseKey)
            _threatRunning.value = false
        }
    }

    // ── Shodan ──
    private val _shodanResult = MutableStateFlow<com.eagleeye.data.ShodanResult?>(null)
    val shodanResult: StateFlow<com.eagleeye.data.ShodanResult?> = _shodanResult.asStateFlow()
    private val _shodanRunning = MutableStateFlow(false)
    val shodanRunning: StateFlow<Boolean> = _shodanRunning.asStateFlow()

    fun runShodanLookup(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _shodanRunning.value = true
            _shodanResult.value = null
            _shodanResult.value = shodanClient.lookup(ip)
            _shodanRunning.value = false
        }
    }
}
