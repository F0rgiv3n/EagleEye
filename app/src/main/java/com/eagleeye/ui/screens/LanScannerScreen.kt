package com.eagleeye.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleeye.data.ScanSnapshot
import com.eagleeye.data.IoTProfile
import com.eagleeye.data.IoTRisk
import com.eagleeye.data.LanDevice
import com.eagleeye.modules.iot.IoTViewModel
import com.eagleeye.modules.lan.LanViewModel
import com.eagleeye.modules.lan.ScanState
import com.eagleeye.modules.wifi.WifiViewModel
import com.eagleeye.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LanFilter { ALL, ONLINE, UNKNOWN, HAS_PORTS }

@Composable
fun LanScannerScreen(viewModel: LanViewModel, iotViewModel: IoTViewModel? = null, wifiViewModel: WifiViewModel? = null) {
    val scanState by viewModel.scanState.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val iotProfiles by (iotViewModel?.profiles ?: kotlinx.coroutines.flow.MutableStateFlow(emptyMap())).collectAsState()
    val iotScanning by (iotViewModel?.scanning ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val scanHistory by viewModel.scanHistory.collectAsState()

    var showTopology by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(LanFilter.ALL) }

    val allDevices = when (val s = scanState) {
        is ScanState.Done -> s.devices
        else -> savedDevices
    }

    val devices = remember(allDevices, searchQuery, activeFilter) {
        allDevices.filter { d ->
            val matchesSearch = searchQuery.isBlank() ||
                d.displayName.contains(searchQuery, ignoreCase = true) ||
                d.ip.contains(searchQuery) ||
                d.mac.contains(searchQuery, ignoreCase = true) ||
                d.vendor.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (activeFilter) {
                LanFilter.ALL      -> true
                LanFilter.ONLINE   -> d.isOnline
                LanFilter.UNKNOWN  -> !d.isKnown
                LanFilter.HAS_PORTS -> d.openPortList.isNotEmpty()
            }
            matchesSearch && matchesFilter
        }
    }

    val connectionInfo by (wifiViewModel?.connectionInfo ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
    val gatewayIp = connectionInfo?.gateway?.takeIf { it.isNotBlank() && it != "0.0.0.0" }
        ?: remember(devices) {
            val firstOnline = devices.firstOrNull { it.isOnline }
            if (firstOnline != null) {
                val parts = firstOnline.ip.split(".")
                if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.1" else "192.168.1.1"
            } else "192.168.1.1"
        }

    if (showHistory && scanHistory.isNotEmpty()) {
        ScanHistoryDialog(
            history = scanHistory,
            onDismiss = { showHistory = false }
        )
    }

    if (showTopology) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {
            TopologyScreen(
                devices = allDevices,
                gatewayIp = gatewayIp,
                onBack = { showTopology = false }
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LAN SCANNER", style = MaterialTheme.typography.headlineMedium, color = CyberGreen)
                Text(
                    text = when (val s = scanState) {
                        is ScanState.Idle -> if (devices.isEmpty()) "No scan yet" else "${devices.size} devices in history"
                        is ScanState.Scanning -> "Scanning... ${(s.progress * 100).toInt()}%"
                        is ScanState.Done -> "${s.devices.size} devices found"
                        is ScanState.Error -> "Error: ${s.message}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (scanHistory.isNotEmpty()) {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "Scan History", tint = CyberYellow)
                    }
                }
                IconButton(onClick = { showTopology = true }) {
                    Icon(Icons.Default.Hub, contentDescription = "Topology Map", tint = CyberBlue)
                }
                if (iotViewModel != null) {
                    OutlinedButton(
                        onClick = { iotViewModel.profileDevices(devices) },
                        enabled = !iotScanning && devices.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberBlue),
                        border = BorderStroke(1.dp, CyberBlue.copy(alpha = if (!iotScanning && devices.isNotEmpty()) 0.5f else 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (iotScanning) {
                            CircularProgressIndicator(Modifier.size(12.dp), CyberBlue, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sensors, null, Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("IoT", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = { viewModel.startScan() },
                    enabled = scanState !is ScanState.Scanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGreen.copy(alpha = 0.15f),
                        contentColor = CyberGreen,
                        disabledContainerColor = SurfaceVariantDark,
                        disabledContentColor = TextDim
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (scanState !is ScanState.Scanning) CyberGreen.copy(0.5f) else TextDim.copy(0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (scanState is ScanState.Scanning) "SCANNING" else "SCAN")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        AnimatedVisibility(visible = scanState is ScanState.Scanning) {
            val progress = (scanState as? ScanState.Scanning)?.progress ?: 0f
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CyberGreen,
                    trackColor = SurfaceVariantDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Probing hosts... ${(progress * 254).toInt()} / 254",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Search bar
        if (allDevices.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, IP, MAC, vendor…", style = MaterialTheme.typography.bodySmall, color = TextDim) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextDim, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, tint = TextDim, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberGreen.copy(alpha = 0.5f),
                    unfocusedBorderColor = CardBorderDark,
                    cursorColor = CyberGreen,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LanFilter.values().forEach { filter ->
                    val selected = activeFilter == filter
                    val (label, color) = when (filter) {
                        LanFilter.ALL      -> "ALL" to CyberGreen
                        LanFilter.ONLINE   -> "ONLINE" to CyberGreen
                        LanFilter.UNKNOWN  -> "UNKNOWN" to CyberOrange
                        LanFilter.HAS_PORTS -> "HAS PORTS" to CyberYellow
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) color else TextDim,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) color.copy(alpha = 0.14f) else SurfaceVariantDark)
                            .border(1.dp, if (selected) color.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(20.dp))
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Stats row
        if (allDevices.isNotEmpty()) {
            val onlineCount = allDevices.count { it.isOnline }
            val unknownCount = allDevices.count { !it.isKnown }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(label = "ONLINE", value = "$onlineCount", color = CyberGreen, modifier = Modifier.weight(1f))
                StatChip(label = "TOTAL", value = "${allDevices.size}", color = CyberBlue, modifier = Modifier.weight(1f))
                StatChip(label = "UNKNOWN", value = "$unknownCount", color = if (unknownCount > 0) CyberOrange else TextDim, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Device list
        if (allDevices.isEmpty() && scanState is ScanState.Idle) {
            EmptyLanState()
        } else if (devices.isEmpty() && allDevices.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No devices match the filter", style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.mac }) { device ->
                    DeviceCard(
                        device = device,
                        viewModel = viewModel,
                        iotProfile = iotProfiles[device.ip]
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryDialog(history: List<ScanSnapshot>, onDismiss: () -> Unit) {
    val sdf = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, null, tint = CyberYellow, modifier = Modifier.size(18.dp))
                Text("Scan History", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.reversed().forEachIndexed { idx, snap ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                sdf.format(java.util.Date(snap.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberYellow
                            )
                            if (idx == 0) {
                                Text("LATEST", style = MaterialTheme.typography.labelSmall, color = CyberGreen)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${snap.totalDevices} devices", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("${snap.onlineDevices} online", style = MaterialTheme.typography.bodySmall, color = CyberGreen)
                            if (snap.newDevices > 0) {
                                Text("+${snap.newDevices} new", style = MaterialTheme.typography.bodySmall, color = CyberOrange)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyberGreen)
            }
        }
    )
}

@Composable
private fun DeviceCard(device: LanDevice, viewModel: LanViewModel, iotProfile: IoTProfile? = null) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var pingState by remember { mutableStateOf<PingState>(PingState.Idle) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val borderColor = when {
        !device.isOnline -> TextDim.copy(alpha = 0.3f)
        !device.isKnown -> CyberOrange.copy(alpha = 0.5f)
        else -> CardBorderDark
    }

    // Rename dialog
    if (showRenameDialog) {
        DeviceRenameDialog(
            currentAlias = device.alias,
            deviceName = device.displayName,
            onConfirm = { alias ->
                viewModel.setAlias(device.mac, alias)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
    ) {
        // Main row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                DeviceIcon(device)

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!device.isKnown) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberOrange,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CyberOrange.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        iotProfile?.let { p ->
                            if (p.riskLevel == IoTRisk.HIGH || p.riskLevel == IoTRisk.MEDIUM) {
                                val riskColor = if (p.riskLevel == IoTRisk.HIGH) CyberRed else CyberOrange
                                Text(
                                    p.riskLevel.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = riskColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(riskColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${device.ip}  •  ${device.mac.ifBlank { "MAC unknown" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    if (device.vendor.isNotBlank() && device.vendor != "Unknown") {
                        Text(
                            text = device.vendor,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberBlue.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (device.isOnline) {
                    Text(
                        text = "${device.latencyMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = latencyColor(device.latencyMs)
                    )
                }
                if (device.openPortList.isNotEmpty()) {
                    Text(
                        text = "${device.openPortList.size} ports",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberYellow
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextDim,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Expanded details
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDark)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(2.dp))

                if (device.hostname.isNotBlank()) {
                    DetailRow("Hostname", device.hostname)
                }
                DetailRow("MAC", device.mac.ifBlank { "Unknown" })
                DetailRow("Vendor", device.vendor.ifBlank { "Unknown" })
                DetailRow("First seen", formatTime(device.firstSeen))
                DetailRow("Last seen", formatTime(device.lastSeen))

                if (device.openPortList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "OPEN PORTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    device.openPortList.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { port ->
                                PortChip(port = port, label = viewModel.getServiceName(port))
                            }
                        }
                    }
                }

                // IoT Profile section
                iotProfile?.let { p ->
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val riskColor = when (p.riskLevel) {
                        IoTRisk.HIGH   -> CyberRed
                        IoTRisk.MEDIUM -> CyberOrange
                        IoTRisk.LOW    -> CyberYellow
                        IoTRisk.SAFE   -> CyberGreen
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("IOT PROFILE", style = MaterialTheme.typography.labelMedium, color = CyberBlue)
                        Text(
                            p.riskLevel.name,
                            style = MaterialTheme.typography.labelMedium, color = riskColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(riskColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("Category", p.category.name.replace('_', ' '))
                    if (p.deviceModel.isNotBlank()) DetailRow("Model", p.deviceModel.take(40))
                    if (p.firmwareHint.isNotBlank()) DetailRow("Firmware", p.firmwareHint)
                    if (p.hasAdminPanel) DetailRow("Admin Panel", "Port ${p.adminPort}")
                    if (p.services.isNotEmpty()) DetailRow("Services", p.services.take(3).joinToString(", "))
                    if (p.hasDefaultCredentials) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberRed.copy(alpha = 0.08f))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = CyberRed, modifier = Modifier.size(12.dp))
                            Text(
                                "Default credentials may apply",
                                style = MaterialTheme.typography.bodySmall, color = CyberRed
                            )
                        }
                    }
                }

                // ── Actions ────────────────────────────────────────────────────
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("ACTIONS", style = MaterialTheme.typography.labelMedium, color = TextDim)
                Spacer(modifier = Modifier.height(4.dp))

                // Action buttons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // Rename
                    DeviceActionBtn(
                        icon = Icons.Default.Edit,
                        label = "Rename",
                        color = CyberBlue
                    ) { showRenameDialog = true }

                    // Ping
                    DeviceActionBtn(
                        icon = Icons.Default.NetworkCheck,
                        label = if (pingState is PingState.Running) "Pinging…" else "Ping",
                        color = CyberGreen,
                        enabled = pingState !is PingState.Running
                    ) {
                        scope.launch {
                            pingState = PingState.Running
                            pingState = withContext(Dispatchers.IO) {
                                try {
                                    val proc = Runtime.getRuntime()
                                        .exec(arrayOf("ping", "-c", "1", "-W", "2", device.ip))
                                    val out = proc.inputStream.bufferedReader().readText()
                                    val ms = Regex("time=(\\d+\\.?\\d*) ms").find(out)?.groupValues?.get(1)
                                    if (ms != null) PingState.Success("${ms}ms")
                                    else PingState.Failed("No reply")
                                } catch (e: Exception) {
                                    PingState.Failed("Error")
                                }
                            }
                        }
                    }

                    // Open web panel (only if web port exists)
                    val webPort = device.openPortList.firstOrNull { it == 443 || it == 80 || it == 8080 || it == 8443 }
                    if (webPort != null) {
                        val scheme = if (webPort == 443 || webPort == 8443) "https" else "http"
                        val url = "$scheme://${device.ip}${if (webPort != 80 && webPort != 443) ":$webPort" else ""}"
                        DeviceActionBtn(
                            icon = Icons.Default.Language,
                            label = "Web Panel",
                            color = CyberYellow
                        ) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }

                    // Copy IP
                    DeviceActionBtn(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy IP",
                        color = TextSecondary
                    ) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("IP Address", device.ip))
                    }

                    // Mark known / unmark
                    if (!device.isKnown) {
                        DeviceActionBtn(
                            icon = Icons.Default.CheckCircle,
                            label = "Mark Known",
                            color = CyberGreen
                        ) { viewModel.markKnown(device.mac, true) }
                    } else {
                        DeviceActionBtn(
                            icon = Icons.Default.RemoveCircleOutline,
                            label = "Unmark",
                            color = TextDim
                        ) { viewModel.markKnown(device.mac, false) }
                    }
                }

                // Ping result banner
                when (val ps = pingState) {
                    is PingState.Success -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberGreen.copy(alpha = 0.08f))
                                .border(1.dp, CyberGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = CyberGreen, modifier = Modifier.size(14.dp))
                            Text("${device.ip} replied in ${ps.ms}", style = MaterialTheme.typography.bodySmall, color = CyberGreen)
                        }
                    }
                    is PingState.Failed -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberRed.copy(alpha = 0.08f))
                                .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                            Text("${device.ip}: ${ps.reason}", style = MaterialTheme.typography.bodySmall, color = CyberRed)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

private sealed class PingState {
    object Idle : PingState()
    object Running : PingState()
    data class Success(val ms: String) : PingState()
    data class Failed(val reason: String) : PingState()
}

@Composable
private fun DeviceActionBtn(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color,
            disabledContentColor = TextDim
        ),
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.35f) else TextDim.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun DeviceRenameDialog(
    currentAlias: String,
    deviceName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentAlias) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                Text("Rename Device", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Device: $deviceName",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Alias", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("e.g. Living Room TV", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberBlue.copy(alpha = 0.6f),
                        unfocusedBorderColor = CardBorderDark,
                        focusedLabelColor = CyberBlue,
                        unfocusedLabelColor = TextDim,
                        cursorColor = CyberBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                if (text.isNotBlank() && text != currentAlias) {
                    Text(
                        "Will show as \"$text\" in all screens",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue.copy(alpha = 0.7f)
                    )
                }
                if (currentAlias.isNotBlank()) {
                    TextButton(
                        onClick = { onConfirm("") },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Clear alias", style = MaterialTheme.typography.labelSmall, color = TextDim)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim() != currentAlias
            ) {
                Text("Save", color = CyberBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextDim)
            }
        }
    )
}

@Composable
private fun DeviceIcon(device: LanDevice) {
    val (icon, tint) = when {
        !device.isOnline -> Icons.Default.SignalWifiOff to TextDim
        device.openPortList.contains(554) -> Icons.Default.Videocam to CyberBlue
        device.openPortList.contains(445) || device.openPortList.contains(139) -> Icons.Default.Computer to CyberBlue
        device.openPortList.contains(22) -> Icons.Default.Terminal to CyberGreen
        device.openPortList.any { it == 80 || it == 443 || it == 8080 } -> Icons.Default.Language to CyberBlue
        else -> Icons.Default.DeviceHub to CyberGreen
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PortChip(port: Int, label: String) {
    val color = when {
        port == 22 -> CyberGreen
        port == 23 -> CyberRed     // Telnet — insecure
        port == 80 -> CyberBlue
        port == 443 -> CyberGreen
        port == 445 -> CyberOrange // SMB
        port == 3389 -> CyberOrange // RDP
        else -> TextSecondary
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$port", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.7f))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatChip(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
    }
}

@Composable
private fun EmptyLanState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.DeviceHub, contentDescription = null, tint = TextDim, modifier = Modifier.size(64.dp))
            Text("No devices scanned", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Text("Tap SCAN to discover devices on your LAN", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
    }
}

private fun latencyColor(ms: Long) = when {
    ms < 10 -> CyberGreen
    ms < 50 -> CyberYellow
    else -> CyberOrange
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
