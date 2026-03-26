package com.eagleeye.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eagleeye.data.IoTProfile
import com.eagleeye.data.IoTRisk
import com.eagleeye.data.LanDevice
import com.eagleeye.modules.iot.IoTViewModel
import com.eagleeye.modules.lan.LanViewModel
import com.eagleeye.modules.lan.ScanState
import com.eagleeye.ui.theme.*

@Composable
fun LanScannerScreen(viewModel: LanViewModel, iotViewModel: IoTViewModel? = null) {
    val scanState by viewModel.scanState.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val iotProfiles by (iotViewModel?.profiles ?: kotlinx.coroutines.flow.MutableStateFlow(emptyMap())).collectAsState()
    val iotScanning by (iotViewModel?.scanning ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    val devices = when (val s = scanState) {
        is ScanState.Done -> s.devices
        else -> savedDevices
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // Stats row
        if (devices.isNotEmpty()) {
            val onlineCount = devices.count { it.isOnline }
            val unknownCount = devices.count { !it.isKnown }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(label = "ONLINE", value = "$onlineCount", color = CyberGreen, modifier = Modifier.weight(1f))
                StatChip(label = "TOTAL", value = "${devices.size}", color = CyberBlue, modifier = Modifier.weight(1f))
                StatChip(label = "UNKNOWN", value = "$unknownCount", color = if (unknownCount > 0) CyberOrange else TextDim, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Device list
        if (devices.isEmpty() && scanState is ScanState.Idle) {
            EmptyLanState()
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
private fun DeviceCard(device: LanDevice, viewModel: LanViewModel, iotProfile: IoTProfile? = null) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = when {
        !device.isOnline -> TextDim.copy(alpha = 0.3f)
        !device.isKnown -> CyberOrange.copy(alpha = 0.5f)
        else -> CardBorderDark
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
                // Status dot
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
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
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
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
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

                Spacer(modifier = Modifier.height(4.dp))
                // Mark as known button
                if (!device.isKnown) {
                    OutlinedButton(
                        onClick = { viewModel.markKnown(device.mac, true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                        border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark as Known Device", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
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
