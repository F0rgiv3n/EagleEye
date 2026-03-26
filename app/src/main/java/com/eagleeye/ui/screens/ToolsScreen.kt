@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.eagleeye.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eagleeye.data.*
import com.eagleeye.modules.tools.ToolsViewModel
import com.eagleeye.modules.packet.PacketViewModel
import com.eagleeye.modules.bluetooth.BluetoothViewModel
import com.eagleeye.ui.theme.*

private enum class Tool { PING, TRACEROUTE, PORT_SCAN, DNS, PUBLIC_IP, WAKE_ON_LAN, SSL, VPN_LEAK, CVE, PORTAL, PACKETS, HEADERS, THREAT_INTEL, SHODAN, BT_SCAN, WHOIS, DHCP, EXPORT, SPEED_TEST, BANDWIDTH, MDNS, ARP, IPV6, DNS_BENCH, FIREWALL, INTERFACES, HTTP_CLIENT, CERT_TRANS }

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    packetViewModel: PacketViewModel? = null,
    btViewModel: BluetoothViewModel? = null,
    wifiInfo: WifiConnectionInfo? = null,
    securityScore: SecurityScore? = null,
    lanDevices: List<LanDevice> = emptyList()
) {
    var selectedTool by remember { mutableStateOf(Tool.PING) }
    val context = LocalContext.current
    val exportIntent by viewModel.exportIntent.collectAsState()

    LaunchedEffect(exportIntent) {
        exportIntent?.let {
            context.startActivity(Intent.createChooser(it, "Share EagleEye Report"))
            viewModel.clearExportIntent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Fixed header — does not scroll away
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("NETWORK TOOLS", style = MaterialTheme.typography.headlineMedium, color = CyberGreen)
            Text("Diagnostics & analysis utilities", style = MaterialTheme.typography.bodySmall, color = TextDim)
            Spacer(modifier = Modifier.height(12.dp))
            ToolTabRow(selected = selectedTool, onSelect = { selectedTool = it })
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Scrollable tool content area
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            when (selectedTool) {
                Tool.PING        -> PingTool(viewModel)
                Tool.TRACEROUTE  -> TracerouteTool(viewModel)
                Tool.PORT_SCAN   -> PortScanTool(viewModel)
                Tool.DNS         -> DnsTool(viewModel)
                Tool.PUBLIC_IP   -> PublicIpTool(viewModel)
                Tool.WAKE_ON_LAN -> WakeOnLanTool(viewModel)
                Tool.SSL         -> SslTool(viewModel)
                Tool.VPN_LEAK    -> VpnLeakTool(viewModel)
                Tool.CVE         -> CveTool(viewModel)
                Tool.PORTAL      -> CaptivePortalTool(viewModel)
                Tool.PACKETS     -> packetViewModel?.let { PacketAnalyzerTool(it) }
                Tool.HEADERS     -> HeadersTool(viewModel)
                Tool.THREAT_INTEL -> ThreatIntelTool(viewModel)
                Tool.SHODAN      -> ShodanTool(viewModel)
                Tool.BT_SCAN     -> BtScanTool(btViewModel)
                Tool.WHOIS       -> WhoisTool(viewModel)
                Tool.DHCP        -> RogueDhcpTool(viewModel)
                Tool.EXPORT      -> ExportTool(viewModel, wifiInfo, securityScore, lanDevices)
                Tool.SPEED_TEST  -> SpeedTestTool(viewModel)
                Tool.BANDWIDTH   -> BandwidthTool(viewModel)
                Tool.MDNS        -> MdnsTool(viewModel)
                Tool.ARP         -> ArpTool(viewModel)
                Tool.IPV6        -> IPv6Tool(viewModel)
                Tool.DNS_BENCH   -> DnsBenchTool(viewModel)
                Tool.FIREWALL    -> FirewallTool(viewModel)
                Tool.INTERFACES  -> InterfacesTool(viewModel)
                Tool.HTTP_CLIENT -> HttpClientTool(viewModel)
                Tool.CERT_TRANS  -> CertTransTool(viewModel)
            }
        }
    }
}

@Composable
private fun ToolTabRow(selected: Tool, onSelect: (Tool) -> Unit) {
    val tools = listOf(
        Tool.PING to (Icons.Default.NetworkPing to "Ping"),
        Tool.TRACEROUTE to (Icons.Default.Route to "Trace"),
        Tool.PORT_SCAN to (Icons.Default.Search to "Ports"),
        Tool.DNS to (Icons.Default.Dns to "DNS"),
        Tool.PUBLIC_IP to (Icons.Default.Public to "IP"),
        Tool.WAKE_ON_LAN to (Icons.Default.Power to "WoL"),
        Tool.SSL to (Icons.Default.Lock to "SSL"),
        Tool.VPN_LEAK to (Icons.Default.VpnKey to "VPN"),
        Tool.CVE to (Icons.Default.BugReport to "CVE"),
        Tool.PORTAL to (Icons.Default.Sensors to "Portal"),
        Tool.PACKETS to (Icons.Default.NetworkCheck to "Packets"),
        Tool.HEADERS to (Icons.Default.Security to "Headers"),
        Tool.THREAT_INTEL to (Icons.Default.GppBad to "Threat"),
        Tool.SHODAN to (Icons.Default.Radar to "Shodan"),
        Tool.BT_SCAN to (Icons.Default.Bluetooth to "BT Scan"),
        Tool.WHOIS       to (Icons.Default.ManageSearch to "WHOIS"),
        Tool.DHCP        to (Icons.Default.Router to "DHCP"),
        Tool.EXPORT      to (Icons.Default.Share to "Export"),
        Tool.SPEED_TEST  to (Icons.Default.Speed to "Speed"),
        Tool.BANDWIDTH   to (Icons.Default.ShowChart to "BW"),
        Tool.MDNS        to (Icons.Default.Cast to "mDNS"),
        Tool.ARP         to (Icons.Default.TableChart to "ARP"),
        Tool.IPV6        to (Icons.Default.Lan to "IPv6"),
        Tool.DNS_BENCH   to (Icons.Default.Timer to "DNS Bench"),
        Tool.FIREWALL    to (Icons.Default.Fireplace to "Firewall"),
        Tool.INTERFACES  to (Icons.Default.AccountTree to "Interfaces"),
        Tool.HTTP_CLIENT to (Icons.Default.Http to "HTTP"),
        Tool.CERT_TRANS  to (Icons.Default.VerifiedUser to "Certs")
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tools.forEach { (tool, pair) ->
            val (icon, label) = pair
            val isSelected = selected == tool
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CyberGreen.copy(alpha = 0.15f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) CyberGreen.copy(alpha = 0.4f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(tool) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    icon, null,
                    tint = if (isSelected) CyberGreen else TextDim,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) CyberGreen else TextDim
                )
            }
        }
    }
}

// ── PING ─────────────────────────────────────────────────────────────────────

@Composable
private fun PingTool(viewModel: ToolsViewModel) {
    var host by remember { mutableStateOf("8.8.8.8") }
    var count by remember { mutableStateOf("8") }
    val result by viewModel.pingResult.collectAsState()
    val running by viewModel.pingRunning.collectAsState()
    val recentHosts by viewModel.recentHosts.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RecentHostsRow(recentHosts) { host = it }
        ToolInputRow(
            value = host, onValueChange = { host = it },
            label = "Host / IP", placeholder = "8.8.8.8",
            trailingContent = {
                OutlinedTextField(
                    value = count, onValueChange = { count = it.filter(Char::isDigit).take(2) },
                    label = { Text("Count", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    colors = cyberTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            onRun = {
                kb?.hide()
                viewModel.runPingWithHistory(host, count.toIntOrNull()?.coerceIn(1, 30) ?: 8)
            },
            running = running
        )

        result?.let { PingResultCard(it) }
        if (running) LoadingCard("Pinging $host...")
    }
}

@Composable
private fun PingResultCard(result: PingResult) {
    val qualityColor = when (result.quality) {
        "Excellent" -> CyberGreen; "Good" -> CyberGreen
        "Fair" -> CyberYellow; "Poor" -> CyberOrange
        else -> CyberRed
    }

    ResultCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(result.host, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(result.quality, style = MaterialTheme.typography.labelMedium, color = qualityColor)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${result.received}/${result.sent} packets",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
                if (result.lostPercent > 0) {
                    Text(
                        "${result.lostPercent}% loss",
                        style = MaterialTheme.typography.bodySmall, color = CyberRed
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (result.minMs >= 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("MIN", "${result.minMs}ms", CyberGreen, Modifier.weight(1f))
                StatBox("AVG", "${result.avgMs}ms", CyberBlue, Modifier.weight(1f))
                StatBox("MAX", "${result.maxMs}ms", CyberYellow, Modifier.weight(1f))
                StatBox("JITTER", "${result.jitterMs}ms", CyberOrange, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            // Samples chart
            Text("SAMPLES", style = MaterialTheme.typography.labelMedium, color = TextDim)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val maxVal = result.samples.filter { it >= 0 }.maxOrNull() ?: 1L
                result.samples.forEach { ms ->
                    val heightFraction = if (ms < 0) 0.05f else (ms.toFloat() / maxVal).coerceIn(0.05f, 1f)
                    val color = if (ms < 0) CyberRed else when {
                        ms < 20 -> CyberGreen; ms < 100 -> CyberYellow; else -> CyberOrange
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * heightFraction).dp)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(color.copy(alpha = if (ms < 0) 0.3f else 0.8f))
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Host unreachable", style = MaterialTheme.typography.bodyMedium, color = CyberRed)
            }
        }
    }
}

// ── TRACEROUTE ────────────────────────────────────────────────────────────────

@Composable
private fun TracerouteTool(viewModel: ToolsViewModel) {
    var host by remember { mutableStateOf("google.com") }
    val hops by viewModel.traceHops.collectAsState()
    val running by viewModel.traceRunning.collectAsState()
    val recentHosts by viewModel.recentHosts.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RecentHostsRow(recentHosts) { host = it }
        ToolInputRow(
            value = host, onValueChange = { host = it },
            label = "Host / IP", placeholder = "google.com",
            onRun = { kb?.hide(); viewModel.runTracerouteWithHistory(host) },
            running = running
        )

        if (running) LoadingCard("Tracing route to $host...")

        if (hops.isNotEmpty()) {
            ResultCard {
                Text("ROUTE (${hops.size} hops)", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
                Spacer(Modifier.height(8.dp))
                hops.forEach { hop -> HopRow(hop) }
            }
        }
    }
}

@Composable
private fun HopRow(hop: TracerouteHop) {
    val isTimeout = hop.isTimeout
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "${hop.index}",
            style = MaterialTheme.typography.labelMedium,
            color = TextDim,
            modifier = Modifier.width(20.dp)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isTimeout) TextDim else CyberGreen)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isTimeout) "* * *" else hop.ip,
                style = MaterialTheme.typography.bodySmall,
                color = if (isTimeout) TextDim else TextPrimary
            )
            if (hop.hostname.isNotBlank()) {
                Text(hop.hostname, style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
        }
        if (!isTimeout) {
            Text(
                "${hop.latencyMs}ms",
                style = MaterialTheme.typography.bodySmall,
                color = latencyColor(hop.latencyMs)
            )
        }
    }
    HorizontalDivider(color = CardBorderDark.copy(alpha = 0.4f), thickness = 0.5.dp)
}

// ── PORT SCAN ─────────────────────────────────────────────────────────────────

@Composable
private fun PortScanTool(viewModel: ToolsViewModel) {
    var host by remember { mutableStateOf("") }
    var quickScan by remember { mutableStateOf(true) }
    val results by viewModel.portResults.collectAsState()
    val progress by viewModel.portScanProgress.collectAsState()
    val running by viewModel.portScanRunning.collectAsState()
    val recentHosts by viewModel.recentHosts.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RecentHostsRow(recentHosts) { host = it }
        ToolInputRow(
            value = host, onValueChange = { host = it },
            label = "Target IP / Host", placeholder = "192.168.1.1",
            onRun = { kb?.hide(); viewModel.runPortScanWithHistory(host, quickScan) },
            running = running
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = quickScan, onCheckedChange = { quickScan = it },
                colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = CyberGreen.copy(alpha = 0.3f))
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    if (quickScan) "Quick Scan (10 ports)" else "Full Scan (35 ports)",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
                Text(
                    if (quickScan) "SSH, HTTP, HTTPS, SMB, RDP..." else "All common services",
                    style = MaterialTheme.typography.bodySmall, color = TextDim
                )
            }
        }

        AnimatedVisibility(visible = running) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = CyberGreen, trackColor = SurfaceVariantDark
            )
        }

        if (!running && results.isEmpty() && host.isNotEmpty()) {
            ResultCard {
                Text("No open ports found", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("All scanned ports are closed or filtered", style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
        }

        if (results.isNotEmpty()) {
            ResultCard {
                Text(
                    "${results.size} OPEN PORT${if (results.size != 1) "S" else ""}",
                    style = MaterialTheme.typography.labelMedium, color = CyberGreen
                )
                Spacer(Modifier.height(8.dp))
                results.forEach { r -> PortRow(r) }
            }
        }
    }
}

@Composable
private fun PortRow(result: PortScanResult) {
    val dangerous = result.port in listOf(23, 21, 4444, 1723)
    val color = if (dangerous) CyberRed else CyberGreen

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${result.port}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Column {
                Text(result.service, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                if (result.banner.isNotBlank()) {
                    Text(result.banner, style = MaterialTheme.typography.bodySmall, color = TextDim, maxLines = 1)
                }
            }
        }
        if (dangerous) {
            Icon(Icons.Default.Warning, null, tint = CyberRed, modifier = Modifier.size(14.dp))
        }
    }
    HorizontalDivider(color = CardBorderDark.copy(alpha = 0.4f), thickness = 0.5.dp)
}

// ── DNS ───────────────────────────────────────────────────────────────────────

@Composable
private fun DnsTool(viewModel: ToolsViewModel) {
    var query by remember { mutableStateOf("") }
    val result by viewModel.dnsResult.collectAsState()
    val running by viewModel.dnsRunning.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputRow(
            value = query, onValueChange = { query = it },
            label = "Domain or IP", placeholder = "google.com or 8.8.8.8",
            onRun = { kb?.hide(); viewModel.runDnsLookup(query) },
            running = running
        )

        if (running) LoadingCard("Resolving $query...")

        result?.let { r ->
            ResultCard {
                DetailRow2("Query", r.query)
                DetailRow2("Type", r.type)
                DetailRow2("DNS Server", r.dnsServer)
                DetailRow2("Resolved in", if (r.resolvedIn >= 0) "${r.resolvedIn}ms" else "Failed")
                Spacer(Modifier.height(8.dp))
                Text("ANSWERS", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
                Spacer(Modifier.height(6.dp))
                r.answers.forEach { answer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceVariantDark)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(answer, style = MaterialTheme.typography.bodySmall, color = CyberBlue, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

// ── PUBLIC IP ─────────────────────────────────────────────────────────────────

@Composable
private fun PublicIpTool(viewModel: ToolsViewModel) {
    val info by viewModel.publicIp.collectAsState()
    val loading by viewModel.ipLoading.collectAsState()

    LaunchedEffect(Unit) {
        if (info == null) viewModel.fetchPublicIp()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("IP INFORMATION", style = MaterialTheme.typography.labelMedium, color = TextDim)
            IconButton(onClick = { viewModel.fetchPublicIp() }, enabled = !loading) {
                Icon(Icons.Default.Refresh, null, tint = if (!loading) CyberGreen else TextDim)
            }
        }

        if (loading) LoadingCard("Fetching IP information...")

        info?.let { ip ->
            ResultCard {
                IpInfoRow(Icons.Default.Public, "Public IPv4", ip.ipv4, CyberGreen)
                IpInfoRow(Icons.Default.Language, "Public IPv6", ip.ipv6.ifBlank { "Not available" }, CyberBlue)
                HorizontalDivider(color = CardBorderDark, modifier = Modifier.padding(vertical = 6.dp))
                IpInfoRow(Icons.Default.PhoneAndroid, "Local IP", ip.localIp, TextSecondary)
                IpInfoRow(Icons.Default.Router, "Gateway", ip.gateway, TextSecondary)
            }
        }
    }
}

@Composable
private fun IpInfoRow(icon: ImageVector, label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = TextDim, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

// ── WAKE ON LAN ───────────────────────────────────────────────────────────────

@Composable
private fun WakeOnLanTool(viewModel: ToolsViewModel) {
    var mac by remember { mutableStateOf("") }
    val status by viewModel.wolStatus.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Send a magic packet to wake a device on the LAN. The target device must have Wake on LAN enabled in BIOS/UEFI.",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )

        ToolInputRow(
            value = mac, onValueChange = { mac = it },
            label = "MAC Address", placeholder = "AA:BB:CC:DD:EE:FF",
            onRun = { kb?.hide(); viewModel.sendWakeOnLan(mac) },
            runLabel = "SEND",
            running = false
        )

        status?.let { msg ->
            val isSuccess = msg.startsWith("Magic")
            ResultCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        tint = if (isSuccess) CyberGreen else CyberRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = if (isSuccess) CyberGreen else CyberRed)
                }
            }
        }
    }
}

// ── Shared UI Components ──────────────────────────────────────────────────────

@Composable
private fun ToolInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    running: Boolean,
    onRun: () -> Unit,
    runLabel: String = "RUN",
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = cyberTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onRun() })
        )
        trailingContent?.invoke()
        Button(
            onClick = onRun,
            enabled = !running,
            modifier = Modifier.height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberGreen.copy(alpha = 0.15f),
                contentColor = CyberGreen,
                disabledContainerColor = SurfaceVariantDark,
                disabledContentColor = TextDim
            ),
            border = BorderStroke(1.dp, if (!running) CyberGreen.copy(0.5f) else TextDim.copy(0.3f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (running) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CyberGreen, strokeWidth = 2.dp)
            } else {
                Text(runLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun LoadingCard(msg: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CyberGreen, strokeWidth = 2.dp)
        Text(msg, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
    }
}

@Composable
private fun DetailRow2(label: String, value: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(label, value))
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied: $label", Toast.LENGTH_SHORT).show()
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun cyberTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberGreen,
    unfocusedBorderColor = CardBorderDark,
    focusedLabelColor = CyberGreen,
    unfocusedLabelColor = TextDim,
    cursorColor = CyberGreen,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextSecondary,
    focusedContainerColor = SurfaceDark,
    unfocusedContainerColor = SurfaceDark
)

// ── SSL INSPECTOR ─────────────────────────────────────────────────────────────

@Composable
private fun SslTool(viewModel: ToolsViewModel) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    val result by viewModel.sslResult.collectAsState()
    val running by viewModel.sslRunning.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = host, onValueChange = { host = it },
                label = { Text("Host / IP", style = MaterialTheme.typography.labelMedium) },
                placeholder = { Text("192.168.1.1") },
                singleLine = true, modifier = Modifier.weight(1f),
                colors = cyberTextFieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) },
                label = { Text("Port", style = MaterialTheme.typography.labelMedium) },
                singleLine = true, modifier = Modifier.width(88.dp),
                colors = cyberTextFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                onClick = { kb?.hide(); viewModel.runSslInspect(host, port.toIntOrNull() ?: 443) },
                enabled = !running && host.isNotBlank(),
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen.copy(alpha = 0.15f), contentColor = CyberGreen,
                    disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
                ),
                border = BorderStroke(1.dp, if (!running && host.isNotBlank()) CyberGreen.copy(0.5f) else TextDim.copy(0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (running) CircularProgressIndicator(Modifier.size(14.dp), CyberGreen, strokeWidth = 2.dp)
                else Text("SCAN", fontWeight = FontWeight.Bold)
            }
        }

        if (running) LoadingCard("Inspecting TLS certificate...")

        result?.let { cert ->
            val gradeColor = when (cert.grade) {
                com.eagleeye.data.SslGrade.A_PLUS -> CyberGreen
                com.eagleeye.data.SslGrade.A      -> CyberGreen
                com.eagleeye.data.SslGrade.B      -> CyberYellow
                com.eagleeye.data.SslGrade.C      -> CyberOrange
                com.eagleeye.data.SslGrade.F,
                com.eagleeye.data.SslGrade.ERROR  -> CyberRed
            }
            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(cert.host, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        if (cert.error != null) {
                            Text(cert.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                            return@ResultCard
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(gradeColor.copy(alpha = 0.12f))
                            .border(2.dp, gradeColor, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cert.grade.name.replace("_", "+"), style = MaterialTheme.typography.labelMedium, color = gradeColor, fontWeight = FontWeight.Bold)
                    }
                }
                if (cert.error != null) return@ResultCard
                Spacer(Modifier.height(10.dp))
                DetailRow2("Subject", cert.subject)
                DetailRow2("Issuer", cert.issuer)
                DetailRow2("Valid From", cert.validFrom)
                DetailRow2("Valid Until", cert.validUntil)
                DetailRow2("Protocol", cert.protocol)
                DetailRow2("Cipher", cert.cipherSuite.take(40))
                Spacer(Modifier.height(8.dp))
                val flags = buildList {
                    if (cert.isExpired) add("EXPIRED" to CyberRed)
                    if (cert.isSelfSigned) add("SELF-SIGNED" to CyberOrange)
                    if (cert.isWeak) add("WEAK CIPHER/PROTOCOL" to CyberOrange)
                    if (!cert.isExpired && cert.daysUntilExpiry in 0..30) add("EXPIRES IN ${cert.daysUntilExpiry}d" to CyberYellow)
                    if (!cert.isExpired && !cert.isSelfSigned && !cert.isWeak) add("VALID" to CyberGreen)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    flags.forEach { (label, color) ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium, color = color,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── VPN LEAK DETECTOR ─────────────────────────────────────────────────────────

@Composable
private fun VpnLeakTool(viewModel: ToolsViewModel) {
    val result by viewModel.vpnLeakResult.collectAsState()
    val running by viewModel.vpnRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("VPN LEAK TEST", style = MaterialTheme.typography.labelMedium, color = TextDim)
                Text("Checks DNS, IPv6 and IP leaks", style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
            Button(
                onClick = { viewModel.runVpnLeakTest() },
                enabled = !running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen.copy(alpha = 0.15f), contentColor = CyberGreen,
                    disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
                ),
                border = BorderStroke(1.dp, if (!running) CyberGreen.copy(0.5f) else TextDim.copy(0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (running) {
                    CircularProgressIndicator(Modifier.size(14.dp), CyberGreen, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("TESTING")
                } else {
                    Icon(Icons.Default.VpnKey, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("TEST")
                }
            }
        }

        if (running) LoadingCard("Testing for DNS, IPv6 and IP leaks...")

        result?.let { r ->
            ResultCard {
                // VPN status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VPN Status", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background((if (r.isVpnActive) CyberGreen else CyberOrange).copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (r.isVpnActive) Icons.Default.VpnKey else Icons.Default.VpnKeyOff,
                            null, tint = if (r.isVpnActive) CyberGreen else CyberOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            if (r.isVpnActive) "ACTIVE" else "NOT ACTIVE",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (r.isVpnActive) CyberGreen else CyberOrange
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                DetailRow2("Public IP", r.publicIpWithVpn)
                if (r.ipv6Address.isNotBlank() && r.ipv6Address != "None") {
                    DetailRow2("IPv6", r.ipv6Address)
                }
                if (r.dnsServers.isNotEmpty()) {
                    DetailRow2("DNS Servers", r.dnsServers.joinToString(", "))
                }
                Spacer(Modifier.height(10.dp))

                // Leak indicators
                val dnsColor = if (r.dnsLeakDetected) CyberRed else CyberGreen
                val ipv6Color = if (r.ipv6LeakDetected) CyberRed else CyberGreen
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeakBadge("DNS", r.dnsLeakDetected)
                    LeakBadge("IPv6", r.ipv6LeakDetected)
                }
                Spacer(Modifier.height(10.dp))

                // Details
                r.leakDetails.forEach { detail ->
                    val isLeak = detail.contains("Leak") || detail.contains("leak")
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Icon(
                            if (isLeak) Icons.Default.Warning else Icons.Default.CheckCircle,
                            null,
                            tint = if (isLeak) CyberRed else CyberGreen,
                            modifier = Modifier.size(14.dp).padding(top = 1.dp)
                        )
                        Text(detail, style = MaterialTheme.typography.bodySmall,
                            color = if (isLeak) CyberRed else TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeakBadge(label: String, leaked: Boolean) {
    val color = if (leaked) CyberRed else CyberGreen
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            if (leaked) Icons.Default.Warning else Icons.Default.CheckCircle,
            null, tint = color, modifier = Modifier.size(12.dp)
        )
        Text(
            "$label ${if (leaked) "LEAK" else "OK"}",
            style = MaterialTheme.typography.labelMedium, color = color
        )
    }
}

// ── CVE LOOKUP ────────────────────────────────────────────────────────────────

@Composable
private fun CveTool(viewModel: ToolsViewModel) {
    var query by remember { mutableStateOf("") }
    val result by viewModel.cveResult.collectAsState()
    val running by viewModel.cveRunning.collectAsState()
    val kb = LocalSoftwareKeyboardController.current
    var severityFilter by remember { mutableStateOf<CveSeverity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Search the NIST NVD for known CVEs by vendor, product, or router model.",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )
        ToolInputRow(
            value = query, onValueChange = { query = it },
            label = "Vendor / Product", placeholder = "TP-Link, Netgear, OpenSSH...",
            onRun = { kb?.hide(); severityFilter = null; viewModel.searchCves(query) },
            running = running
        )

        if (running) LoadingCard("Querying NIST NVD database...")

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(16.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
            } else {
                // Summary row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${r.totalFound} CVEs for \"${r.query}\"",
                        style = MaterialTheme.typography.labelMedium, color = TextDim
                    )
                    Text(
                        "showing ${r.entries.size}",
                        style = MaterialTheme.typography.labelMedium, color = TextDim
                    )
                }

                // Severity filter chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CveSeverityChip("ALL", null, severityFilter, TextDim) { severityFilter = null }
                    CveSeverityChip("CRITICAL", CveSeverity.CRITICAL, severityFilter, CyberRed) { severityFilter = CveSeverity.CRITICAL }
                    CveSeverityChip("HIGH", CveSeverity.HIGH, severityFilter, CyberOrange) { severityFilter = CveSeverity.HIGH }
                    CveSeverityChip("MEDIUM", CveSeverity.MEDIUM, severityFilter, CyberYellow) { severityFilter = CveSeverity.MEDIUM }
                    CveSeverityChip("LOW", CveSeverity.LOW, severityFilter, CyberBlue) { severityFilter = CveSeverity.LOW }
                }

                val filtered = if (severityFilter == null) r.entries
                               else r.entries.filter { it.severity == severityFilter }

                if (filtered.isEmpty()) {
                    ResultCard {
                        Text(
                            "No ${severityFilter?.name} CVEs in current results.",
                            style = MaterialTheme.typography.bodySmall, color = TextDim
                        )
                    }
                } else {
                    filtered.forEach { cve -> CveCard(cve) }
                }
            }
        }
    }
}

@Composable
private fun CveSeverityChip(
    label: String,
    severity: CveSeverity?,
    selected: CveSeverity?,
    color: Color,
    onClick: () -> Unit
) {
    val isSelected = severity == selected
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) color.copy(alpha = 0.18f) else SurfaceDark)
            .border(1.dp, if (isSelected) color.copy(alpha = 0.7f) else CardBorderDark, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) color else TextDim,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CveCard(cve: com.eagleeye.data.CveEntry) {
    var expanded by remember { mutableStateOf(false) }
    val scoreColor = when (cve.severity) {
        com.eagleeye.data.CveSeverity.CRITICAL -> CyberRed
        com.eagleeye.data.CveSeverity.HIGH     -> CyberOrange
        com.eagleeye.data.CveSeverity.MEDIUM   -> CyberYellow
        com.eagleeye.data.CveSeverity.LOW      -> CyberBlue
        com.eagleeye.data.CveSeverity.NONE     -> TextDim
    }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, scoreColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("CVE ID", cve.id))
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied: ${cve.id}", Toast.LENGTH_SHORT).show()
                }
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cve.id, style = MaterialTheme.typography.bodySmall, color = scoreColor, fontWeight = FontWeight.Bold)
                Text(cve.description.take(80) + if (cve.description.length > 80) "..." else "",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    "%.1f".format(cve.cvssScore),
                    style = MaterialTheme.typography.titleMedium, color = scoreColor, fontWeight = FontWeight.Bold
                )
                Text(cve.severity.name, style = MaterialTheme.typography.labelMedium, color = scoreColor)
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().background(CardDark).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(color = scoreColor.copy(alpha = 0.2f))
                Text(cve.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                DetailRow2("Published", cve.publishedDate)
                DetailRow2("CVSS Score", "%.1f / 10.0 (${cve.severity})".format(cve.cvssScore))
                if (cve.references.isNotEmpty()) {
                    Text("References", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    cve.references.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = CyberBlue.copy(alpha = 0.8f), maxLines = 1)
                    }
                }
            }
        }
    }
}

// ── CAPTIVE PORTAL ANALYZER ───────────────────────────────────────────────────

@Composable
private fun CaptivePortalTool(viewModel: ToolsViewModel) {
    val result by viewModel.portalResult.collectAsState()
    val running by viewModel.portalRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Checks whether this network intercepts traffic via a captive portal " +
                "and analyses it for suspicious behaviour.",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CAPTIVE PORTAL CHECK", style = MaterialTheme.typography.labelMedium, color = TextDim)
            Button(
                onClick = { viewModel.runPortalCheck() },
                enabled = !running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen.copy(alpha = 0.15f), contentColor = CyberGreen,
                    disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
                ),
                border = BorderStroke(1.dp, if (!running) CyberGreen.copy(0.5f) else TextDim.copy(0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (running) {
                    CircularProgressIndicator(Modifier.size(14.dp), CyberGreen, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("CHECKING")
                } else {
                    Icon(Icons.Default.Sensors, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CHECK")
                }
            }
        }

        if (running) LoadingCard("Probing connectivity endpoints...")

        result?.let { r ->
            val (statusLabel, statusColor) = when (r.status) {
                com.eagleeye.data.PortalStatus.NONE       -> "NO PORTAL" to CyberGreen
                com.eagleeye.data.PortalStatus.DETECTED   -> "PORTAL DETECTED" to CyberYellow
                com.eagleeye.data.PortalStatus.SUSPICIOUS -> "SUSPICIOUS PORTAL" to CyberRed
                com.eagleeye.data.PortalStatus.ERROR      -> "ERROR" to TextDim
            }

            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Network Portal Status", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            r.checkedUrl,
                            style = MaterialTheme.typography.bodySmall, color = TextDim, maxLines = 1
                        )
                    }
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelMedium, color = statusColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (r.status == com.eagleeye.data.PortalStatus.ERROR && r.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                    return@ResultCard
                }

                if (r.status != com.eagleeye.data.PortalStatus.NONE) {
                    Spacer(Modifier.height(10.dp))

                    if (r.portalUrl.isNotBlank()) {
                        DetailRow2("Portal URL", r.portalUrl.take(60))
                    }
                    if (r.pageTitle.isNotBlank()) {
                        DetailRow2("Page Title", r.pageTitle)
                    }
                    if (r.responseCode > 0) {
                        DetailRow2("Response Code", "${r.responseCode}")
                    }

                    if (r.redirectChain.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("REDIRECT CHAIN (${r.redirectChain.size} hops)", style = MaterialTheme.typography.labelMedium, color = CyberYellow)
                        Spacer(Modifier.height(4.dp))
                        r.redirectChain.forEachIndexed { i, url ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = TextDim, modifier = Modifier.width(16.dp))
                                Text(url.take(55), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    if (r.suspicionReasons.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("SUSPICIOUS INDICATORS", style = MaterialTheme.typography.labelMedium, color = CyberRed)
                        Spacer(Modifier.height(4.dp))
                        r.suspicionReasons.forEach { reason ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Warning, null, tint = CyberRed, modifier = Modifier.size(13.dp))
                                Text(reason, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                            }
                        }
                    }

                    if (r.hasCertIssue) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberRed.copy(alpha = 0.08f))
                                .border(1.dp, CyberRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                            Text(
                                "SSL certificate issue — possible MITM attack",
                                style = MaterialTheme.typography.bodySmall, color = CyberRed
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                        Text("Network traffic is not being intercepted", style = MaterialTheme.typography.bodySmall, color = CyberGreen)
                    }
                }
            }
        }
    }
}

private fun latencyColor(ms: Long) = when {
    ms < 0 -> TextDim
    ms < 20 -> CyberGreen
    ms < 80 -> CyberYellow
    else -> CyberOrange
}

// ── HTTP SECURITY HEADERS ─────────────────────────────────────────────────────

@Composable
private fun HeadersTool(viewModel: ToolsViewModel) {
    var url by remember { mutableStateOf("https://") }
    val result by viewModel.headersResult.collectAsState()
    val running by viewModel.headersRunning.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputRow(
            value = url, onValueChange = { url = it },
            label = "URL", placeholder = "https://example.com",
            onRun = { kb?.hide(); viewModel.runHeadersCheck(url) },
            running = running
        )

        if (running) LoadingCard("Analyzing HTTP security headers...")

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Text(r.url, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
                return@let
            }

            val gradeColor = when (r.grade) {
                com.eagleeye.data.HeaderGrade.A_PLUS -> CyberGreen
                com.eagleeye.data.HeaderGrade.A      -> CyberGreen
                com.eagleeye.data.HeaderGrade.B      -> CyberYellow
                com.eagleeye.data.HeaderGrade.C      -> CyberOrange
                com.eagleeye.data.HeaderGrade.F,
                com.eagleeye.data.HeaderGrade.ERROR  -> CyberRed
            }
            val gradeLabel = when (r.grade) {
                com.eagleeye.data.HeaderGrade.A_PLUS -> "A+"
                else -> r.grade.name
            }

            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.url.take(50), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        if (r.responseCode > 0) {
                            Text("HTTP ${r.responseCode}", style = MaterialTheme.typography.labelMedium, color = TextDim)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(gradeColor.copy(alpha = 0.12f))
                            .border(2.dp, gradeColor, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(gradeLabel, style = MaterialTheme.typography.titleSmall, color = gradeColor, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Score: ${r.score}/100", style = MaterialTheme.typography.bodySmall, color = gradeColor)
                Spacer(Modifier.height(10.dp))
                Text("SECURITY HEADERS", style = MaterialTheme.typography.labelMedium, color = TextDim)
                Spacer(Modifier.height(6.dp))

                r.headers.forEach { entry -> SecurityHeaderRow(entry) }

                if (r.infoLeaks.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("INFO LEAKS", style = MaterialTheme.typography.labelMedium, color = CyberYellow)
                    Spacer(Modifier.height(4.dp))
                    r.infoLeaks.forEach { leak ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = CyberYellow, modifier = Modifier.size(13.dp))
                            Text(leak, style = MaterialTheme.typography.bodySmall, color = CyberYellow)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                val recommendation = when (r.grade) {
                    com.eagleeye.data.HeaderGrade.A_PLUS -> "Excellent security headers configuration."
                    com.eagleeye.data.HeaderGrade.A      -> "Good configuration. Minor improvements possible."
                    com.eagleeye.data.HeaderGrade.B      -> "Moderate security. Several headers missing or weak."
                    com.eagleeye.data.HeaderGrade.C      -> "Poor security. Add critical missing headers."
                    com.eagleeye.data.HeaderGrade.F,
                    com.eagleeye.data.HeaderGrade.ERROR  -> "Critical: Most security headers are absent."
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(gradeColor.copy(alpha = 0.07f))
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = gradeColor, modifier = Modifier.size(14.dp))
                    Text(recommendation, style = MaterialTheme.typography.bodySmall, color = gradeColor)
                }
            }
        }
    }
}

@Composable
private fun SecurityHeaderRow(entry: com.eagleeye.data.SecurityHeaderEntry) {
    val statusColor = when (entry.status) {
        com.eagleeye.data.HeaderStatus.PRESENT -> CyberGreen
        com.eagleeye.data.HeaderStatus.WEAK    -> CyberOrange
        com.eagleeye.data.HeaderStatus.MISSING -> CyberRed
    }
    val statusLabel = when (entry.status) {
        com.eagleeye.data.HeaderStatus.PRESENT -> "PRESENT"
        com.eagleeye.data.HeaderStatus.WEAK    -> "WEAK"
        com.eagleeye.data.HeaderStatus.MISSING -> "MISSING"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(entry.description, style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "${entry.points}pt",
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
    HorizontalDivider(color = CardBorderDark.copy(alpha = 0.3f), thickness = 0.5.dp)
}

// ── THREAT INTELLIGENCE ───────────────────────────────────────────────────────

@Composable
private fun ThreatIntelTool(viewModel: ToolsViewModel) {
    var ip by remember { mutableStateOf("") }
    var abuseKey by remember { mutableStateOf("") }
    var showAbuseKey by remember { mutableStateOf(false) }
    val result by viewModel.threatResult.collectAsState()
    val running by viewModel.threatRunning.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputRow(
            value = ip, onValueChange = { ip = it },
            label = "IP Address", placeholder = "1.2.3.4",
            onRun = { kb?.hide(); viewModel.runThreatIntel(ip, abuseKey) },
            running = running
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariantDark)
                .clickable { showAbuseKey = !showAbuseKey }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Add AbuseIPDB Key (optional)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Icon(
                if (showAbuseKey) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = TextDim, modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = showAbuseKey) {
            OutlinedTextField(
                value = abuseKey,
                onValueChange = { abuseKey = it },
                label = { Text("AbuseIPDB API Key", style = MaterialTheme.typography.labelMedium) },
                placeholder = { Text("Paste your key here") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = cyberTextFieldColors()
            )
        }

        if (running) LoadingCard("Checking threat intelligence...")

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
                return@let
            }

            val riskColor = when (r.riskLevel) {
                "SAFE"      -> CyberGreen
                "SUSPICIOUS" -> CyberOrange
                "HIGH RISK" -> CyberRed
                else        -> TextDim
            }

            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(r.ip, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Threat Intelligence", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(riskColor.copy(alpha = 0.14f))
                            .border(1.dp, riskColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            when (r.riskLevel) {
                                "SAFE" -> Icons.Default.CheckCircle
                                "HIGH RISK" -> Icons.Default.Dangerous
                                else -> Icons.Default.Warning
                            },
                            null, tint = riskColor, modifier = Modifier.size(13.dp)
                        )
                        Text(r.riskLevel, style = MaterialTheme.typography.labelMedium, color = riskColor, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (r.country.isNotBlank()) DetailRow2("Country", "${r.country} (${r.countryCode})")
                if (r.city.isNotBlank()) DetailRow2("City", r.city)
                if (r.isp.isNotBlank()) DetailRow2("ISP", r.isp)
                if (r.org.isNotBlank()) DetailRow2("Org", r.org)
                if (r.asn.isNotBlank()) DetailRow2("ASN", r.asn)

                if (r.isProxy || r.isHosting) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (r.isProxy) {
                            Text(
                                "PROXY/VPN",
                                style = MaterialTheme.typography.labelSmall, color = CyberOrange,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(CyberOrange.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (r.isHosting) {
                            Text(
                                "HOSTING/DATACENTER",
                                style = MaterialTheme.typography.labelSmall, color = CyberYellow,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(CyberYellow.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (r.riskReasons.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("RISK INDICATORS", style = MaterialTheme.typography.labelMedium, color = riskColor)
                    Spacer(Modifier.height(4.dp))
                    r.riskReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = riskColor, modifier = Modifier.size(12.dp))
                            Text(reason, style = MaterialTheme.typography.bodySmall, color = riskColor)
                        }
                    }
                }

                if (r.abuseScore >= 0) {
                    Spacer(Modifier.height(10.dp))
                    Text("ABUSEIPDB", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    Spacer(Modifier.height(6.dp))
                    val abuseColor = when {
                        r.abuseScore >= 75 -> CyberRed
                        r.abuseScore >= 25 -> CyberOrange
                        else -> CyberGreen
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { r.abuseScore / 100f },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = abuseColor,
                            trackColor = SurfaceVariantDark
                        )
                        Text("${r.abuseScore}%", style = MaterialTheme.typography.labelMedium, color = abuseColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${r.abuseReports} reports in last 90 days", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (r.abuseCategories.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            r.abuseCategories.forEach { cat ->
                                Text(
                                    cat, style = MaterialTheme.typography.labelSmall, color = CyberRed,
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                        .background(CyberRed.copy(alpha = 0.10f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── SHODAN INTERNETDB ─────────────────────────────────────────────────────────

@Composable
private fun ShodanTool(viewModel: ToolsViewModel) {
    var ip by remember { mutableStateOf("") }
    val result by viewModel.shodanResult.collectAsState()
    val running by viewModel.shodanRunning.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputRow(
            value = ip, onValueChange = { ip = it },
            label = "IP Address", placeholder = "Use your public IP or any IPv4",
            onRun = { kb?.hide(); viewModel.runShodanLookup(ip) },
            running = running
        )

        if (running) LoadingCard("Querying Shodan InternetDB...")

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
                return@let
            }

            val hasData = r.ports.isNotEmpty() || r.hostnames.isNotEmpty() || r.cves.isNotEmpty() || r.tags.isNotEmpty()

            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(r.ip, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.Radar, null, tint = CyberBlue, modifier = Modifier.size(20.dp))
                }

                if (!hasData) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.SearchOff, null, tint = TextDim, modifier = Modifier.size(14.dp))
                        Text("This IP has no data in Shodan's database", style = MaterialTheme.typography.bodySmall, color = TextDim)
                    }
                } else {
                    if (r.ports.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("OPEN PORTS (${r.ports.size})", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            r.ports.chunked(6).forEach { rowPorts ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rowPorts.forEach { port ->
                                        Text(
                                            "$port",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CyberGreen,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CyberGreen.copy(alpha = 0.10f))
                                                .border(1.dp, CyberGreen.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (r.hostnames.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("HOSTNAMES", style = MaterialTheme.typography.labelMedium, color = CyberBlue)
                        Spacer(Modifier.height(4.dp))
                        r.hostnames.forEach { hostname ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Language, null, tint = CyberBlue, modifier = Modifier.size(12.dp))
                                Text(hostname, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    if (r.cves.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("CVEs (${r.cves.size})", style = MaterialTheme.typography.labelMedium, color = CyberRed)
                        Spacer(Modifier.height(4.dp))
                        r.cves.forEach { cve ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    cve,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberRed,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberRed.copy(alpha = 0.10f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (r.tags.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("TAGS", style = MaterialTheme.typography.labelMedium, color = CyberYellow)
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            r.tags.forEach { tag ->
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberYellow,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberYellow.copy(alpha = 0.10f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = TextDim, modifier = Modifier.size(12.dp))
                    Text("Data from Shodan InternetDB — passive data only", style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
            }
        }
    }
}

// ── BLUETOOTH SCANNER ─────────────────────────────────────────────────────────

@Composable
private fun BtScanTool(viewModel: BluetoothViewModel?) {
    if (viewModel == null) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Bluetooth not available", color = TextDim)
        }
        return
    }

    val devices by viewModel.devices.collectAsState()
    val scanning by viewModel.scanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header + button
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("BLUETOOTH SCANNER", style = MaterialTheme.typography.labelMedium, color = TextDim)
                Text(
                    if (scanning) "Scanning... (auto-stops at 15s)" else "${devices.size} devices found",
                    style = MaterialTheme.typography.bodySmall, color = TextDim
                )
            }
            Button(
                onClick = { if (scanning) viewModel.stopScan() else viewModel.startScan() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = (if (scanning) CyberRed else CyberGreen).copy(alpha = 0.15f),
                    contentColor = if (scanning) CyberRed else CyberGreen,
                    disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
                ),
                border = BorderStroke(1.dp, (if (scanning) CyberRed else CyberGreen).copy(0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (scanning) {
                    CircularProgressIndicator(Modifier.size(14.dp), CyberGreen, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("STOP")
                } else {
                    Icon(Icons.Default.Bluetooth, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SCAN")
                }
            }
        }

        scanError?.let { err ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(CyberRed.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                Text(err, style = MaterialTheme.typography.bodySmall, color = CyberRed)
            }
        }

        // Stats row
        if (devices.isNotEmpty()) {
            val bleCount = devices.count { it.isBle }
            val classicCount = devices.count { !it.isBle }
            val bondedCount = devices.count { it.bondState == "BONDED" }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("BLE", "$bleCount", CyberBlue, Modifier.weight(1f))
                StatBox("CLASSIC", "$classicCount", CyberGreen, Modifier.weight(1f))
                StatBox("BONDED", "$bondedCount", CyberOrange, Modifier.weight(1f))
            }
        }

        // Device list
        if (devices.isEmpty() && !scanning) {
            ResultCard {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bluetooth, null, tint = TextDim, modifier = Modifier.size(32.dp))
                        Text("No devices found", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Tap SCAN to discover nearby Bluetooth devices", style = MaterialTheme.typography.bodySmall, color = TextDim)
                    }
                }
            }
        }

        devices.forEach { device -> BtDeviceCard(device) }
    }
}

@Composable
private fun BtDeviceCard(device: com.eagleeye.data.BtDevice) {
    var expanded by remember { mutableStateOf(false) }

    val rssiColor = when {
        device.rssi >= -60 -> CyberGreen
        device.rssi >= -75 -> CyberYellow
        device.rssi >= -85 -> CyberOrange
        else -> CyberRed
    }
    val rssiLabel = when {
        device.rssi >= -60 -> "Strong"
        device.rssi >= -75 -> "Good"
        device.rssi >= -85 -> "Weak"
        else -> "Poor"
    }

    val (typeIcon, typeColor) = when (device.deviceType) {
        com.eagleeye.data.BtDeviceType.PHONE      -> Icons.Default.PhoneAndroid to CyberGreen
        com.eagleeye.data.BtDeviceType.COMPUTER   -> Icons.Default.Computer to CyberBlue
        com.eagleeye.data.BtDeviceType.HEADPHONES -> Icons.Default.Hearing to CyberBlue
        com.eagleeye.data.BtDeviceType.SPEAKER    -> Icons.Default.Speaker to CyberBlue
        com.eagleeye.data.BtDeviceType.KEYBOARD   -> Icons.Default.Keyboard to CyberYellow
        com.eagleeye.data.BtDeviceType.MOUSE      -> Icons.Default.DragHandle to CyberYellow
        com.eagleeye.data.BtDeviceType.WEARABLE   -> Icons.Default.Watch to CyberGreen
        com.eagleeye.data.BtDeviceType.TV         -> Icons.Default.Tv to CyberBlue
        com.eagleeye.data.BtDeviceType.PRINTER    -> Icons.Default.Print to TextSecondary
        com.eagleeye.data.BtDeviceType.CAR        -> Icons.Default.DirectionsCar to CyberOrange
        com.eagleeye.data.BtDeviceType.HEALTH     -> Icons.Default.Favorite to CyberRed
        else -> Icons.Default.Bluetooth to TextDim
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(typeIcon, null, tint = typeColor, modifier = Modifier.size(18.dp))
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(device.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                        if (device.bondState == "BONDED") {
                            Icon(Icons.Default.Link, null, tint = CyberGreen, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            if (device.isBle) "BLE" else "Classic",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (device.isBle) CyberBlue else CyberGreen,
                            modifier = Modifier.clip(RoundedCornerShape(3.dp))
                                .background((if (device.isBle) CyberBlue else CyberGreen).copy(alpha = 0.1f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(device.address, style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${device.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = rssiColor, fontWeight = FontWeight.Medium)
                Text(rssiLabel, style = MaterialTheme.typography.labelMedium, color = rssiColor)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier.fillMaxWidth().background(CardDark).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
                Spacer(Modifier.height(2.dp))
                DetailRow2("Type", device.deviceType.name.replace('_', ' '))
                if (device.deviceClass.isNotBlank()) DetailRow2("Class", device.deviceClass)
                DetailRow2("Bond State", device.bondState)
                DetailRow2("Protocol", if (device.isBle) "Bluetooth Low Energy" else "Bluetooth Classic")
                if (device.manufacturerName.isNotBlank()) DetailRow2("Manufacturer", device.manufacturerName)
                if (device.txPower != Int.MIN_VALUE) DetailRow2("TX Power", "${device.txPower} dBm")

                val distance = if (device.txPower != Int.MIN_VALUE) {
                    val ratio = device.rssi.toDouble() / device.txPower
                    if (ratio < 1.0) Math.pow(ratio, 10.0)
                    else (0.89976 * Math.pow(ratio, 7.7095) + 0.111)
                } else null
                if (distance != null) {
                    DetailRow2("Est. Distance", "~${"%.1f".format(distance)}m")
                }
            }
        }
    }
}

// ── WHOIS + Reverse DNS ───────────────────────────────────────────────────────

@Composable
private fun WhoisTool(viewModel: ToolsViewModel) {
    val result by viewModel.whoisResult.collectAsState()
    val running by viewModel.whoisRunning.collectAsState()
    val history by viewModel.whoisHistory.collectAsState()
    var query by remember { mutableStateOf("") }
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RecentHostsRow(history, label = "Recent", color = CyberBlue) { query = it }
        ToolInputRow(
            value = query, onValueChange = { query = it },
            label = "Domain or IP",
            placeholder = "example.com or 8.8.8.8",
            running = running,
            onRun = { kb?.hide(); viewModel.runWhoisWithHistory(query.trim()) },
            runLabel = "LOOKUP"
        )

        result?.let { r ->
            if (r.error != null) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(CyberRed.copy(alpha = 0.08f)).border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                    Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                }
                return@let
            }
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark).border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(r.query, style = MaterialTheme.typography.titleSmall,
                    color = CyberGreen, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = CardBorderDark)

                if (r.reverseDns.isNotBlank() && r.reverseDns != "—")
                    WhoisRow("Reverse DNS", r.reverseDns, CyberBlue)
                if (r.org.isNotBlank())     WhoisRow("Organisation", r.org)
                if (r.country.isNotBlank()) WhoisRow("Country", r.country)
                if (r.netblock.isNotBlank()) WhoisRow("Netblock", r.netblock)
                if (!r.isIpLookup) {
                    if (r.registrar.isNotBlank()) WhoisRow("Registrar", r.registrar)
                    if (r.created.isNotBlank())   WhoisRow("Created", r.created)
                    if (r.updated.isNotBlank())   WhoisRow("Updated", r.updated)
                    r.nameServer.forEachIndexed { i, ns ->
                        WhoisRow("NS ${i + 1}", ns, TextDim)
                    }
                }

                // Raw WHOIS toggle
                var showRaw by remember { mutableStateOf(false) }
                TextButton(onClick = { showRaw = !showRaw }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (showRaw) "Hide raw" else "Show raw WHOIS",
                        style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
                if (showRaw) {
                    Text(
                        r.raw.lines().filter { !it.startsWith("%") && it.isNotBlank() }
                            .take(40).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace),
                        color = TextDim,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(SurfaceVariantDark).padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WhoisRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextDim,
            modifier = Modifier.weight(0.4f))
        Text(value.take(60), style = MaterialTheme.typography.bodySmall, color = valueColor,
            modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Medium)
    }
}

// ── Rogue DHCP Detector ───────────────────────────────────────────────────────

@Composable
private fun RogueDhcpTool(viewModel: ToolsViewModel) {
    val result  by viewModel.rogueDhcpResult.collectAsState()
    val running by viewModel.rogueDhcpRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Description
        Text("Scans for rogue DHCP servers and suspicious gateway mismatches on your LAN.",
            style = MaterialTheme.typography.bodySmall, color = TextDim)

        Button(
            onClick = { viewModel.runRogueDhcpScan() },
            enabled = !running,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberOrange.copy(alpha = 0.15f), contentColor = CyberOrange,
                disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
            ),
            border = BorderStroke(1.dp, if (!running) CyberOrange.copy(alpha = 0.5f) else TextDim.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
        ) {
            if (running) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberOrange, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("SCANNING SUBNET...")
            } else {
                Icon(Icons.Default.Router, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("SCAN FOR ROGUE DHCP", fontWeight = FontWeight.Bold)
            }
        }

        result?.let { r ->
            if (r.error != null) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(CyberRed.copy(alpha = 0.08f)).border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                    Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                }
                return@let
            }

            // Status banner
            val (bannerColor, bannerIcon, bannerText) = if (r.isClean)
                Triple(CyberGreen, Icons.Default.CheckCircle, "No rogue DHCP servers detected")
            else Triple(CyberRed, Icons.Default.Warning, "${r.findings.size} suspicious finding${if (r.findings.size != 1) "s" else ""} detected")

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(bannerColor.copy(alpha = 0.08f))
                    .border(1.dp, bannerColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(bannerIcon, null, tint = bannerColor, modifier = Modifier.size(18.dp))
                Text(bannerText, style = MaterialTheme.typography.bodyMedium, color = bannerColor,
                    fontWeight = FontWeight.Bold)
            }

            // Network info
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark).border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WhoisRow("Gateway", r.knownGateway + if (r.gatewayHostname.isNotBlank()) " (${r.gatewayHostname})" else "")
                WhoisRow("DHCP Server", r.dhcpServer,
                    if (r.serverGatewayMismatch) CyberOrange else CyberGreen)
                if (r.suspiciousServers.isNotEmpty())
                    WhoisRow("Suspicious IPs", r.suspiciousServers.joinToString(", "), CyberRed)
            }

            // Findings
            if (r.findings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.findings.forEach { finding ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(CyberRed.copy(alpha = 0.06f))
                                .border(1.dp, CyberRed.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = CyberRed, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                            Text(finding, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

// ── Export / Report ───────────────────────────────────────────────────────────

@Composable
private fun ExportTool(
    viewModel: ToolsViewModel,
    wifiInfo: WifiConnectionInfo?,
    score: SecurityScore?,
    devices: List<LanDevice>
) {
    val hasWifi    = wifiInfo?.isConnected == true
    val hasScore   = score != null
    val hasDevices = devices.isNotEmpty()
    val canExport  = hasWifi || hasScore || hasDevices

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Preview card ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("REPORT CONTENTS",
                style = MaterialTheme.typography.labelMedium,
                color = CyberGreen)
            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

            ReportSection(
                icon = Icons.Default.Wifi,
                label = "Network",
                value = if (hasWifi) wifiInfo?.ssid ?: "Unknown" else "No connection data",
                available = hasWifi
            )
            ReportSection(
                icon = Icons.Default.Shield,
                label = "Security Audit",
                value = if (hasScore) score?.let { "Grade ${it.grade}  ·  ${it.threats.size} threats  ·  ${it.total}/100" } ?: "Score unavailable"
                        else "No audit data — run Security scan first",
                available = hasScore
            )
            ReportSection(
                icon = Icons.Default.DeviceHub,
                label = "LAN Devices",
                value = if (hasDevices) "${devices.size} device${if (devices.size != 1) "s" else ""}  ·  ${devices.count { it.isOnline }} online"
                        else "No scan data — run LAN scan first",
                available = hasDevices
            )
            ReportSection(
                icon = Icons.Default.History,
                label = "Event History",
                value = "Last 100 events included automatically",
                available = true
            )
        }

        if (!canExport) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberOrange.copy(alpha = 0.08f))
                    .border(1.dp, CyberOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = CyberOrange, modifier = Modifier.size(16.dp))
                Text("Run a LAN scan and Security audit to populate the report.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberOrange)
            }
        }

        // ── Export buttons ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val wifi = wifiInfo ?: WifiConnectionInfo()
            Button(
                onClick = { viewModel.exportReport(wifi, score, devices, asJson = true) },
                enabled = canExport,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen.copy(alpha = 0.15f),
                    contentColor = CyberGreen,
                    disabledContainerColor = SurfaceVariantDark,
                    disabledContentColor = TextDim
                ),
                border = BorderStroke(1.dp, if (canExport) CyberGreen.copy(alpha = 0.5f) else TextDim.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("JSON", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.exportReport(wifi, score, devices, asJson = false) },
                enabled = canExport,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberBlue.copy(alpha = 0.15f),
                    contentColor = CyberBlue,
                    disabledContainerColor = SurfaceVariantDark,
                    disabledContentColor = TextDim
                ),
                border = BorderStroke(1.dp, if (canExport) CyberBlue.copy(alpha = 0.5f) else TextDim.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Article, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("TEXT", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }

        // Format info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariantDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("JSON", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = CyberGreen)
            Text("Machine-readable · import into SIEM tools · full event log", style = MaterialTheme.typography.bodySmall, color = TextDim)
            Spacer(Modifier.height(4.dp))
            Text("TEXT", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = CyberBlue)
            Text("Human-readable · share via email/chat · formatted report", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
    }
}

@Composable
private fun ReportSection(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, available: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null,
            tint = if (available) CyberGreen else TextDim,
            modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (available) TextSecondary else TextDim)
            Text(value, style = MaterialTheme.typography.bodySmall, color = if (available) TextPrimary else TextDim)
        }
        if (available) {
            Icon(Icons.Default.Check, null, tint = CyberGreen, modifier = Modifier.size(14.dp).padding(top = 2.dp))
        }
    }
}

// ── SPEED TEST ────────────────────────────────────────────────────────────────

@Composable
private fun SpeedTestTool(viewModel: ToolsViewModel) {
    val progress by viewModel.speedProgress.collectAsState()
    val result   by viewModel.speedResult.collectAsState()
    val running  by viewModel.speedRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Measures download/upload speed and latency via Cloudflare.",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )

        Button(
            onClick = { viewModel.runSpeedTest() },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberGreen.copy(alpha = 0.12f),
                contentColor   = CyberGreen,
                disabledContainerColor = SurfaceVariantDark,
                disabledContentColor   = TextDim
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, if (!running) CyberGreen.copy(alpha = 0.5f) else TextDim.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (running) {
                CircularProgressIndicator(Modifier.size(14.dp), CyberGreen, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Default.Speed, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                when (progress.phase) {
                    com.eagleeye.data.SpeedPhase.PINGING      -> "PINGING…"
                    com.eagleeye.data.SpeedPhase.DOWNLOADING  -> "DOWNLOADING… ${"%.1f".format(progress.currentMbps)} Mbps"
                    com.eagleeye.data.SpeedPhase.UPLOADING    -> "UPLOADING… ${"%.1f".format(progress.currentMbps)} Mbps"
                    else -> "RUN SPEED TEST"
                },
                fontWeight = FontWeight.Bold
            )
        }

        if (running && progress.phase != com.eagleeye.data.SpeedPhase.IDLE) {
            LinearProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = CyberGreen, trackColor = SurfaceVariantDark
            )
        }

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(16.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
            } else {
                // Big 3 stat boxes
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeedStatBox("DOWNLOAD", "${"%.1f".format(r.downloadMbps)}", "Mbps", CyberGreen, Modifier.weight(1f))
                    SpeedStatBox("UPLOAD",   "${"%.1f".format(r.uploadMbps)}",   "Mbps", CyberBlue,  Modifier.weight(1f))
                    SpeedStatBox("PING",     if (r.pingMs > 0) "${r.pingMs}" else "—", "ms", CyberYellow, Modifier.weight(1f))
                }
                ResultCard {
                    DetailRow2("Server", r.server)
                }
            }
        }
    }
}

@Composable
private fun SpeedStatBox(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        Text(unit,  style = MaterialTheme.typography.labelSmall,    color = color.copy(alpha = 0.7f))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,    color = TextDim)
    }
}

// ── BANDWIDTH MONITOR ─────────────────────────────────────────────────────────

@Composable
private fun BandwidthTool(viewModel: ToolsViewModel) {
    val samples by viewModel.bandwidthSamples.collectAsState()
    val active  by viewModel.bandwidthActive.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Live RX/TX traffic monitor using Android TrafficStats.",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )

        Button(
            onClick = { if (active) viewModel.stopBandwidthMonitor() else viewModel.startBandwidthMonitor() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) CyberRed.copy(alpha = 0.1f) else CyberGreen.copy(alpha = 0.1f),
                contentColor   = if (active) CyberRed else CyberGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (active) CyberRed.copy(alpha = 0.4f) else CyberGreen.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(if (active) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (active) "STOP" else "START MONITOR", fontWeight = FontWeight.Bold)
        }

        if (samples.isNotEmpty()) {
            val last = samples.last()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BwStatBox("↓ RX", formatBytes(last.rxSpeed), CyberGreen, Modifier.weight(1f))
                BwStatBox("↑ TX", formatBytes(last.txSpeed), CyberBlue,  Modifier.weight(1f))
            }
            BandwidthChart(samples)
        }
    }
}

@Composable
private fun BwStatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), maxLines = 1)
    }
}

@Composable
private fun BandwidthChart(samples: List<com.eagleeye.data.BandwidthSample>) {
    val maxRx = samples.maxOf { it.rxSpeed }.coerceAtLeast(1f)
    val maxTx = samples.maxOf { it.txSpeed }.coerceAtLeast(1f)
    val maxVal = maxOf(maxRx, maxTx)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("RX/TX over time", style = MaterialTheme.typography.labelMedium, color = TextDim)
            Text("peak ${formatBytes(maxVal)}/s", style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(80.dp)
        ) {
            val w = size.width
            val h = size.height
            if (samples.size < 2) return@Canvas
            val step = w / (samples.size - 1)

            fun drawLine(getValue: (com.eagleeye.data.BandwidthSample) -> Float, color: Color) {
                val path = androidx.compose.ui.graphics.Path()
                samples.forEachIndexed { i, s ->
                    val x = i * step
                    val y = h - (getValue(s) / maxVal) * h
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            }

            drawLine({ it.rxSpeed }, CyberGreen)
            drawLine({ it.txSpeed }, CyberBlue)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.foundation.Canvas(Modifier.size(8.dp)) { drawCircle(CyberGreen) }
                Text("Download", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.foundation.Canvas(Modifier.size(8.dp)) { drawCircle(CyberBlue) }
                Text("Upload", style = MaterialTheme.typography.labelSmall, color = TextDim)
            }
        }
    }
}

private fun formatBytes(bytesPerSec: Float): String = when {
    bytesPerSec >= 1_000_000f -> "${"%.1f".format(bytesPerSec / 1_000_000f)} MB/s"
    bytesPerSec >= 1_000f     -> "${"%.0f".format(bytesPerSec / 1_000f)} KB/s"
    else                      -> "${bytesPerSec.toInt()} B/s"
}

// ── MDNS / SERVICE DISCOVERY ──────────────────────────────────────────────────

@Composable
private fun MdnsTool(viewModel: ToolsViewModel) {
    val services by viewModel.mdnsServices.collectAsState()
    val running  by viewModel.mdnsRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Discovers services on the local network: Chromecast, printers, SSH, file shares, and more.",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )
        Button(
            onClick = { if (running) viewModel.stopMdnsDiscovery() else viewModel.startMdnsDiscovery() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (running) CyberRed.copy(alpha = 0.1f) else CyberGreen.copy(alpha = 0.1f),
                contentColor   = if (running) CyberRed else CyberGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (running) CyberRed.copy(alpha = 0.4f) else CyberGreen.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (running) CircularProgressIndicator(Modifier.size(14.dp), CyberRed, strokeWidth = 2.dp)
            else Icon(Icons.Default.Cast, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (running) "DISCOVERING… (${services.size} found)" else "START DISCOVERY", fontWeight = FontWeight.Bold)
        }

        if (services.isEmpty() && !running) {
            Text("No services found yet. Start discovery on a Wi-Fi network.",
                style = MaterialTheme.typography.bodySmall, color = TextDim)
        }

        services.forEach { svc -> MdnsServiceCard(svc) }
    }
}

@Composable
private fun MdnsServiceCard(svc: com.eagleeye.data.MdnsService) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(CyberBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Cast, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(svc.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(svc.friendlyType, style = MaterialTheme.typography.bodySmall, color = CyberBlue)
            if (svc.ip.isNotBlank()) Text("${svc.ip}:${svc.port}", style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
        Text(":${svc.port}", style = MaterialTheme.typography.labelMedium, color = CyberYellow)
    }
}

// ── ARP CACHE ─────────────────────────────────────────────────────────────────

@Composable
private fun ArpTool(viewModel: ToolsViewModel) {
    val entries  by viewModel.arpEntries.collectAsState()
    val running  by viewModel.interfacesRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ARP cache from /proc/net/arp", style = MaterialTheme.typography.bodySmall, color = TextDim)
            OutlinedButton(
                onClick = { viewModel.refreshInterfaces() },
                enabled = !running,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (running) CircularProgressIndicator(Modifier.size(12.dp), CyberGreen, strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Refresh", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (entries.isEmpty() && !running) {
            Text("Tap Refresh to read ARP table.", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }

        entries.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceDark)
                    .border(1.dp, if (entry.isComplete) CardBorderDark else CyberOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(entry.ip, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(entry.mac, style = MaterialTheme.typography.labelSmall, color = TextDim,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.iface, style = MaterialTheme.typography.labelSmall, color = CyberBlue)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background((if (entry.isComplete) CyberGreen else CyberOrange).copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (entry.isComplete) "COMPLETE" else entry.flags,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.isComplete) CyberGreen else CyberOrange
                        )
                    }
                }
            }
        }
    }
}

// ── IPv6 INSPECTOR ────────────────────────────────────────────────────────────

@Composable
private fun IPv6Tool(viewModel: ToolsViewModel) {
    val result  by viewModel.ipv6Result.collectAsState()
    val running by viewModel.ipv6Running.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Inspects IPv6 addresses and tests connectivity.", style = MaterialTheme.typography.bodySmall, color = TextDim)

        Button(
            onClick = { viewModel.runIPv6Inspect() },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberBlue.copy(alpha = 0.1f), contentColor = CyberBlue,
                disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (!running) CyberBlue.copy(alpha = 0.4f) else TextDim.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (running) CircularProgressIndicator(Modifier.size(14.dp), CyberBlue, strokeWidth = 2.dp)
            else Icon(Icons.Default.Lan, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (running) "INSPECTING…" else "INSPECT IPv6", fontWeight = FontWeight.Bold)
        }

        if (running) LoadingCard("Testing IPv6 connectivity…")

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(16.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
            } else {
                val statusColor = when {
                    r.hasConnectivity  -> CyberGreen
                    r.hasGlobalAddress -> CyberYellow
                    else               -> CyberOrange
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.08f))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (r.hasConnectivity) Icons.Default.CheckCircle else Icons.Default.Warning,
                        null, tint = statusColor, modifier = Modifier.size(16.dp)
                    )
                    Text(r.note, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
                ResultCard {
                    if (r.globalAddress.isNotBlank()) DetailRow2("Global IPv6", r.globalAddress)
                    if (r.linkLocalAddress.isNotBlank()) DetailRow2("Link-local", r.linkLocalAddress)
                    DetailRow2("Connectivity", if (r.hasConnectivity) "Yes" else "No")
                    DetailRow2("Addresses found", "${r.addresses.size}")
                }
                if (r.addresses.isNotEmpty()) {
                    r.addresses.forEach { addr ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorderDark, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(addr.address.take(32), style = MaterialTheme.typography.labelSmall, color = TextSecondary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text(addr.type, style = MaterialTheme.typography.labelSmall, color = CyberBlue)
                        }
                    }
                }
            }
        }
    }
}

// ── DNS BENCHMARK ─────────────────────────────────────────────────────────────

@Composable
private fun DnsBenchTool(viewModel: ToolsViewModel) {
    val result  by viewModel.dnsBenchResult.collectAsState()
    val running by viewModel.dnsBenchRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Benchmarks 6 public DNS resolvers with 5 UDP probes each.", style = MaterialTheme.typography.bodySmall, color = TextDim)

        Button(
            onClick = { viewModel.runDnsBenchmark() },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberYellow.copy(alpha = 0.1f), contentColor = CyberYellow,
                disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (!running) CyberYellow.copy(alpha = 0.4f) else TextDim.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (running) CircularProgressIndicator(Modifier.size(14.dp), CyberYellow, strokeWidth = 2.dp)
            else Icon(Icons.Default.Timer, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (running) "BENCHMARKING…" else "RUN BENCHMARK", fontWeight = FontWeight.Bold)
        }

        if (running) LoadingCard("Sending DNS probes to 6 servers…")

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(16.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
            } else {
                val maxAvg = r.results.filter { it.successRate > 0 }.maxOfOrNull { it.avgMs }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                Text("Fastest: ${r.fastest}", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
                r.results.forEach { dns ->
                    val barColor = when (dns.rank) { 1 -> CyberGreen; 2 -> CyberBlue; else -> TextDim }
                    val barFrac  = if (dns.successRate == 0) 0f else (dns.avgMs.toFloat() / maxAvg).coerceIn(0.05f, 1f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.dp, if (dns.rank == 1) CyberGreen.copy(alpha = 0.4f) else CardBorderDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("#${dns.rank}", style = MaterialTheme.typography.labelMedium, color = barColor,
                            fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(dns.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text(
                                    if (dns.successRate == 0) "TIMEOUT" else "${dns.avgMs}ms avg",
                                    style = MaterialTheme.typography.bodySmall, color = barColor
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { barFrac },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = barColor, trackColor = SurfaceVariantDark
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(dns.server, style = MaterialTheme.typography.labelSmall, color = TextDim)
                                Text("${dns.successRate}% ok · min ${dns.minMs}ms", style = MaterialTheme.typography.labelSmall, color = TextDim)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── FIREWALL TESTER ───────────────────────────────────────────────────────────

@Composable
private fun FirewallTool(viewModel: ToolsViewModel) {
    val result   by viewModel.firewallResult.collectAsState()
    val progress by viewModel.firewallProgress.collectAsState()
    val running  by viewModel.firewallRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Tests if common ports are blocked by your ISP or router (via portquiz.net).",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )
        Button(
            onClick = { viewModel.runFirewallTest() },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberOrange.copy(alpha = 0.1f), contentColor = CyberOrange,
                disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (!running) CyberOrange.copy(alpha = 0.4f) else TextDim.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (running) CircularProgressIndicator(Modifier.size(14.dp), CyberOrange, strokeWidth = 2.dp)
            else Icon(Icons.Default.Fireplace, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (running) "TESTING ${progress.first}/${progress.second}…" else "TEST PORTS",
                fontWeight = FontWeight.Bold
            )
        }

        if (running && progress.second > 0) {
            LinearProgressIndicator(
                progress = { progress.first.toFloat() / progress.second },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = CyberOrange, trackColor = SurfaceVariantDark
            )
        }

        result?.let { r ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpeedStatBox("OPEN",    "${r.openCount}",    "ports", CyberGreen,  Modifier.weight(1f))
                SpeedStatBox("BLOCKED", "${r.blockedCount}", "ports", CyberRed,    Modifier.weight(1f))
                SpeedStatBox("TESTED",  "${r.results.size}", "total", TextSecondary, Modifier.weight(1f))
            }
            r.results.chunked(3).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowItems.forEach { p ->
                        val color = if (p.isOpen) CyberGreen else CyberRed
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.07f))
                                .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${p.port}", style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
                            Text(p.service, style = MaterialTheme.typography.labelSmall, color = TextDim, maxLines = 1)
                            Text(if (p.isOpen) "${p.latencyMs}ms" else "blocked",
                                style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
                        }
                    }
                    // fill empty slots
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ── NETWORK INTERFACES ────────────────────────────────────────────────────────

@Composable
private fun InterfacesTool(viewModel: ToolsViewModel) {
    val interfaces by viewModel.interfaces.collectAsState()
    val running    by viewModel.interfacesRunning.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("All active network interfaces.", style = MaterialTheme.typography.bodySmall, color = TextDim)
            OutlinedButton(
                onClick = { viewModel.refreshInterfaces() },
                enabled = !running,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (running) CircularProgressIndicator(Modifier.size(12.dp), CyberGreen, strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Refresh", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (interfaces.isEmpty() && !running) {
            Text("Tap Refresh to list interfaces.", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }

        interfaces.forEach { iface ->
            val typeColor = when (iface.type) {
                "Wi-Fi"    -> CyberGreen
                "Cellular" -> CyberBlue
                "VPN"      -> CyberYellow
                "Ethernet" -> CyberBlue
                "Loopback" -> TextDim
                else       -> TextSecondary
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (iface.isUp) SurfaceDark else SurfaceDark.copy(alpha = 0.5f))
                    .border(1.dp, if (iface.isUp) typeColor.copy(alpha = 0.3f) else CardBorderDark, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(iface.friendlyName, style = MaterialTheme.typography.bodyMedium, color = if (iface.isUp) TextPrimary else TextDim, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(iface.type, style = MaterialTheme.typography.labelSmall, color = typeColor,
                            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(typeColor.copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 2.dp))
                        Text(if (iface.isUp) "UP" else "DOWN",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (iface.isUp) CyberGreen else CyberRed,
                            modifier = Modifier.clip(RoundedCornerShape(3.dp))
                                .background((if (iface.isUp) CyberGreen else CyberRed).copy(alpha = 0.1f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                if (iface.ipv4.isNotBlank()) DetailRow2("IPv4", iface.ipv4)
                if (iface.ipv6.isNotBlank()) DetailRow2("IPv6", iface.ipv6.take(36))
                if (iface.mac.isNotBlank()) DetailRow2("MAC", iface.mac)
                if (iface.mtu > 0) DetailRow2("MTU", "${iface.mtu} bytes")
            }
        }
    }
}

// ── RECENT HOSTS ROW ──────────────────────────────────────────────────────────

@Composable
private fun RecentHostsRow(
    hosts: List<String>,
    label: String = "Recent",
    color: Color = CyberGreen,
    onSelect: (String) -> Unit
) {
    if (hosts.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
        hosts.forEach { host ->
            Text(
                host,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.1f))
                    .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                    .clickable { onSelect(host) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ── HTTP CLIENT ───────────────────────────────────────────────────────────────

@Composable
private fun HttpClientTool(viewModel: ToolsViewModel) {
    var url by remember { mutableStateOf("https://") }
    var method by remember { mutableStateOf("GET") }
    var body by remember { mutableStateOf("") }
    var showBody by remember { mutableStateOf(false) }
    var customHeader by remember { mutableStateOf("") }
    val result by viewModel.httpResult.collectAsState()
    val running by viewModel.httpRunning.collectAsState()
    val kb = LocalSoftwareKeyboardController.current
    val methods = listOf("GET", "POST", "HEAD", "PUT", "DELETE")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Send HTTP requests and inspect responses, headers, and status codes.",
            style = MaterialTheme.typography.bodySmall, color = TextDim)

        // Method selector
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            methods.forEach { m ->
                val selected = method == m
                val mColor = when (m) {
                    "GET"    -> CyberGreen
                    "POST"   -> CyberBlue
                    "DELETE" -> CyberRed
                    else     -> CyberYellow
                }
                Text(
                    m,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) mColor else TextDim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) mColor.copy(alpha = 0.15f) else SurfaceVariantDark)
                        .border(1.dp, if (selected) mColor.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { method = m; showBody = m in listOf("POST", "PUT") }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // URL field
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = cyberTextFieldColors(),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            )
        )

        // Body (POST/PUT)
        AnimatedVisibility(visible = showBody || method == "POST" || method == "PUT") {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Request Body", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = cyberTextFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Button(
            onClick = { kb?.hide(); viewModel.runHttpRequest(url, method, body) },
            enabled = !running && url.length > 8,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberGreen.copy(alpha = 0.12f), contentColor = CyberGreen,
                disabledContainerColor = SurfaceVariantDark, disabledContentColor = TextDim
            ),
            border = BorderStroke(1.dp, if (!running && url.length > 8) CyberGreen.copy(alpha = 0.4f) else TextDim.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (running) CircularProgressIndicator(Modifier.size(14.dp), CyberGreen, strokeWidth = 2.dp)
            else Icon(Icons.Default.Http, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (running) "SENDING…" else "SEND $method", fontWeight = FontWeight.Bold)
        }

        if (running) LoadingCard("Sending $method request...")

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
                return@let
            }
            val statusColor = when (r.statusCode) {
                in 200..299 -> CyberGreen
                in 300..399 -> CyberBlue
                in 400..499 -> CyberOrange
                else        -> CyberRed
            }
            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${r.statusCode}",
                                style = MaterialTheme.typography.titleLarge,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(r.statusMessage, style = MaterialTheme.typography.bodySmall, color = statusColor)
                        }
                        Text(r.url.take(50), style = MaterialTheme.typography.labelSmall, color = TextDim)
                    }
                    Text(
                        "${r.durationMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (r.durationMs < 500) CyberGreen else if (r.durationMs < 2000) CyberYellow else CyberOrange
                    )
                }

                if (r.responseHeaders.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    var showHeaders by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showHeaders = !showHeaders },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            if (showHeaders) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = TextDim, modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${if (showHeaders) "Hide" else "Show"} headers (${r.responseHeaders.size})",
                            style = MaterialTheme.typography.bodySmall, color = TextDim
                        )
                    }
                    AnimatedVisibility(visible = showHeaders) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            r.responseHeaders.entries.take(20).forEach { (k, v) ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(k, style = MaterialTheme.typography.labelSmall, color = CyberBlue, modifier = Modifier.weight(0.4f))
                                    Text(v.take(80), style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(0.6f))
                                }
                            }
                        }
                    }
                }

                if (r.responseBody.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    var showBody2 by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showBody2 = !showBody2 },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            if (showBody2) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = TextDim, modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${if (showBody2) "Hide" else "Show"} body (${r.responseBody.length} chars)",
                            style = MaterialTheme.typography.bodySmall, color = TextDim
                        )
                    }
                    AnimatedVisibility(visible = showBody2) {
                        Text(
                            r.responseBody.take(2000),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = TextDim,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariantDark)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── CERTIFICATE TRANSPARENCY ──────────────────────────────────────────────────

@Composable
private fun CertTransTool(viewModel: ToolsViewModel) {
    var domain by remember { mutableStateOf("") }
    val result by viewModel.certTransResult.collectAsState()
    val running by viewModel.certTransRunning.collectAsState()
    val recentHosts by viewModel.recentHosts.collectAsState()
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Queries crt.sh certificate transparency logs to find all SSL/TLS certificates issued for a domain.",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )

        RecentHostsRow(recentHosts.filter { !it.startsWith("192") && !it.startsWith("10.") }) { domain = it }

        ToolInputRow(
            value = domain, onValueChange = { domain = it },
            label = "Domain", placeholder = "example.com",
            running = running,
            onRun = {
                kb?.hide()
                viewModel.addRecentHost(domain)
                viewModel.runCertTransparency(domain)
            },
            runLabel = "SEARCH"
        )

        if (running) LoadingCard("Querying crt.sh transparency logs...")

        result?.let { r ->
            if (r.error != null) {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                        Text(r.error, style = MaterialTheme.typography.bodySmall, color = CyberRed)
                    }
                }
                return@let
            }

            if (r.entries.isEmpty()) {
                ResultCard {
                    Text("No certificates found for ${r.domain}", style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
                return@let
            }

            ResultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(r.domain, style = MaterialTheme.typography.titleSmall, color = CyberGreen, fontWeight = FontWeight.Bold)
                    Text(
                        "${r.entries.size} certs",
                        style = MaterialTheme.typography.labelMedium, color = CyberBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))

                r.entries.take(30).forEach { cert ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceVariantDark)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(cert.commonName, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
                        if (cert.nameValue.isNotBlank() && cert.nameValue != cert.commonName) {
                            Text(cert.nameValue.take(60), style = MaterialTheme.typography.labelSmall, color = CyberBlue)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cert.issuerCn.take(40), style = MaterialTheme.typography.labelSmall, color = TextDim)
                            Text("${cert.notBefore} → ${cert.notAfter}", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (r.entries.size > 30) {
                    Text(
                        "… and ${r.entries.size - 30} more",
                        style = MaterialTheme.typography.bodySmall, color = TextDim
                    )
                }
            }
        }
    }
}
