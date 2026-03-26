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
import kotlinx.coroutines.Job
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
    private val speedTestClient = SpeedTestClient()
    private val bandwidthMonitor = BandwidthMonitor()
    private val mdnsDiscovery = MdnsDiscovery(application)
    private val ifaceScanner = NetworkInterfaceScanner()
    private val dnsBenchmark = DnsBenchmark()
    private val firewallTester = FirewallTester()

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

    // ── Speed Test ──
    private val _speedProgress = MutableStateFlow(SpeedTestProgress())
    val speedProgress: StateFlow<SpeedTestProgress> = _speedProgress.asStateFlow()
    private val _speedResult = MutableStateFlow<SpeedTestResult?>(null)
    val speedResult: StateFlow<SpeedTestResult?> = _speedResult.asStateFlow()
    private val _speedRunning = MutableStateFlow(false)
    val speedRunning: StateFlow<Boolean> = _speedRunning.asStateFlow()

    fun runSpeedTest() {
        viewModelScope.launch(Dispatchers.IO) {
            _speedRunning.value = true
            _speedResult.value = null
            _speedProgress.value = SpeedTestProgress()
            _speedResult.value = speedTestClient.runTest { progress ->
                _speedProgress.value = progress
            }
            _speedRunning.value = false
        }
    }

    // ── Bandwidth Monitor ──
    private val _bandwidthSamples = MutableStateFlow<List<BandwidthSample>>(emptyList())
    val bandwidthSamples: StateFlow<List<BandwidthSample>> = _bandwidthSamples.asStateFlow()
    private val _bandwidthActive = MutableStateFlow(false)
    val bandwidthActive: StateFlow<Boolean> = _bandwidthActive.asStateFlow()
    private var bandwidthJob: Job? = null

    fun startBandwidthMonitor() {
        if (bandwidthJob?.isActive == true) return
        _bandwidthActive.value = true
        _bandwidthSamples.value = emptyList()
        bandwidthJob = viewModelScope.launch {
            bandwidthMonitor.observe().collect { sample ->
                _bandwidthSamples.value = (_bandwidthSamples.value + sample).takeLast(60)
            }
        }
    }

    fun stopBandwidthMonitor() {
        bandwidthJob?.cancel()
        _bandwidthActive.value = false
    }

    // ── mDNS Discovery ──
    private val _mdnsServices = MutableStateFlow<List<MdnsService>>(emptyList())
    val mdnsServices: StateFlow<List<MdnsService>> = _mdnsServices.asStateFlow()
    private val _mdnsRunning = MutableStateFlow(false)
    val mdnsRunning: StateFlow<Boolean> = _mdnsRunning.asStateFlow()
    private var mdnsJob: Job? = null

    fun startMdnsDiscovery() {
        if (mdnsJob?.isActive == true) return
        _mdnsServices.value = emptyList()
        _mdnsRunning.value = true
        mdnsJob = viewModelScope.launch {
            mdnsDiscovery.discover().collect { service ->
                val existing = _mdnsServices.value
                if (existing.none { it.name == service.name && it.type == service.type }) {
                    _mdnsServices.value = existing + service
                }
            }
        }
    }

    fun stopMdnsDiscovery() {
        mdnsJob?.cancel()
        _mdnsRunning.value = false
    }

    // ── Network Interfaces + ARP ──
    private val _interfaces = MutableStateFlow<List<NetworkInterfaceInfo>>(emptyList())
    val interfaces: StateFlow<List<NetworkInterfaceInfo>> = _interfaces.asStateFlow()
    private val _arpEntries = MutableStateFlow<List<ArpCacheEntry>>(emptyList())
    val arpEntries: StateFlow<List<ArpCacheEntry>> = _arpEntries.asStateFlow()
    private val _interfacesRunning = MutableStateFlow(false)
    val interfacesRunning: StateFlow<Boolean> = _interfacesRunning.asStateFlow()

    fun refreshInterfaces() {
        viewModelScope.launch(Dispatchers.IO) {
            _interfacesRunning.value = true
            _interfaces.value = ifaceScanner.getInterfaces()
            _arpEntries.value = ifaceScanner.getArpCache()
            _interfacesRunning.value = false
        }
    }

    // ── IPv6 Inspector ──
    private val _ipv6Result = MutableStateFlow<IPv6InspectorResult?>(null)
    val ipv6Result: StateFlow<IPv6InspectorResult?> = _ipv6Result.asStateFlow()
    private val _ipv6Running = MutableStateFlow(false)
    val ipv6Running: StateFlow<Boolean> = _ipv6Running.asStateFlow()

    fun runIPv6Inspect() {
        viewModelScope.launch(Dispatchers.IO) {
            _ipv6Running.value = true
            _ipv6Result.value = null
            _ipv6Result.value = ifaceScanner.inspectIPv6()
            _ipv6Running.value = false
        }
    }

    // ── DNS Benchmark ──
    private val _dnsBenchResult = MutableStateFlow<DnsBenchmarkResult?>(null)
    val dnsBenchResult: StateFlow<DnsBenchmarkResult?> = _dnsBenchResult.asStateFlow()
    private val _dnsBenchRunning = MutableStateFlow(false)
    val dnsBenchRunning: StateFlow<Boolean> = _dnsBenchRunning.asStateFlow()

    fun runDnsBenchmark() {
        viewModelScope.launch(Dispatchers.IO) {
            _dnsBenchRunning.value = true
            _dnsBenchResult.value = null
            _dnsBenchResult.value = dnsBenchmark.run()
            _dnsBenchRunning.value = false
        }
    }

    // ── Firewall Tester ──
    private val _firewallResult = MutableStateFlow<FirewallTestResult?>(null)
    val firewallResult: StateFlow<FirewallTestResult?> = _firewallResult.asStateFlow()
    private val _firewallProgress = MutableStateFlow(0 to 0)
    val firewallProgress: StateFlow<Pair<Int, Int>> = _firewallProgress.asStateFlow()
    private val _firewallRunning = MutableStateFlow(false)
    val firewallRunning: StateFlow<Boolean> = _firewallRunning.asStateFlow()

    fun runFirewallTest() {
        viewModelScope.launch(Dispatchers.IO) {
            _firewallRunning.value = true
            _firewallResult.value = null
            _firewallProgress.value = 0 to FirewallTester.TEST_PORTS.size
            _firewallResult.value = firewallTester.run { done, total ->
                _firewallProgress.value = done to total
            }
            _firewallRunning.value = false
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
