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
import com.eagleeye.ui.theme.*

private enum class Tool { PING, TRACEROUTE, PORT_SCAN, DNS, PUBLIC_IP, WAKE_ON_LAN }

@Composable
fun ToolsScreen(viewModel: ToolsViewModel) {
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
        Tool.WAKE_ON_LAN to (Icons.Default.Power to "WoL")
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tools.forEach { (tool, pair) ->
            val (icon, label) = pair
            val isSelected = selected == tool
            Column(
                modifier = Modifier
                    .weight(1f)
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

private fun latencyColor(ms: Long) = when {
    ms < 0 -> TextDim
    ms < 20 -> CyberGreen
    ms < 80 -> CyberYellow
    else -> CyberOrange
}
