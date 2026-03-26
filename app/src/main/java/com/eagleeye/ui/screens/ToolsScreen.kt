package com.eagleeye.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eagleeye.data.*
import com.eagleeye.modules.tools.ToolsViewModel
import com.eagleeye.modules.packet.PacketViewModel
import com.eagleeye.modules.bluetooth.BluetoothViewModel
import com.eagleeye.ui.theme.*

private enum class Tool { PING, TRACEROUTE, PORT_SCAN, DNS, PUBLIC_IP, WAKE_ON_LAN, SSL, VPN_LEAK, CVE, PORTAL, PACKETS, HEADERS, THREAT_INTEL, SHODAN, BT_SCAN }

@Composable
fun ToolsScreen(viewModel: ToolsViewModel, packetViewModel: PacketViewModel? = null, btViewModel: BluetoothViewModel? = null) {
    var selectedTool by remember { mutableStateOf(Tool.PING) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        Text("NETWORK TOOLS", style = MaterialTheme.typography.headlineMedium, color = CyberGreen)
        Text("Diagnostics & analysis utilities", style = MaterialTheme.typography.bodySmall, color = TextDim)

        Spacer(modifier = Modifier.height(12.dp))

        // Tool selector tabs
        ToolTabRow(selected = selectedTool, onSelect = { selectedTool = it })

        Spacer(modifier = Modifier.height(16.dp))

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
        Tool.BT_SCAN to (Icons.Default.Bluetooth to "BT Scan")
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
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                viewModel.runPing(host, count.toIntOrNull()?.coerceIn(1, 30) ?: 8)
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
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputRow(
            value = host, onValueChange = { host = it },
            label = "Host / IP", placeholder = "google.com",
            onRun = { kb?.hide(); viewModel.runTraceroute(host) },
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
    val kb = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputRow(
            value = host, onValueChange = { host = it },
            label = "Target IP / Host", placeholder = "192.168.1.1",
            onRun = { kb?.hide(); viewModel.runPortScan(host, quickScan) },
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Search the NIST NVD for known CVEs by vendor, product, or router model.",
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )
        ToolInputRow(
            value = query, onValueChange = { query = it },
            label = "Vendor / Product", placeholder = "TP-Link, Netgear, OpenSSH...",
            onRun = { kb?.hide(); viewModel.searchCves(query) },
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
                Text(
                    "${r.totalFound} CVEs found for \"${r.query}\" — showing ${r.entries.size}",
                    style = MaterialTheme.typography.labelMedium, color = TextDim
                )
                r.entries.forEach { cve -> CveCard(cve) }
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, scoreColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
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
