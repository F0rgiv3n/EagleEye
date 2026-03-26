package com.eagleeye.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eagleeye.data.ScannedNetwork
import com.eagleeye.data.SecurityGrade
import com.eagleeye.modules.wifi.WifiViewModel
import com.eagleeye.ui.theme.*

@Composable
fun NetworkScanScreen(viewModel: WifiViewModel) {
    val scanResults by viewModel.scanResults.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val currentInfo by viewModel.connectionInfo.collectAsState()
    var showSpectrum by remember { mutableStateOf(false) }

    if (showSpectrum) {
        SpectrumScreen(
            networks = scanResults,
            currentSsid = currentInfo.ssid,
            onBack = { showSpectrum = false }
        )
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
                Text("NETWORK SCAN", style = MaterialTheme.typography.headlineMedium, color = CyberGreen)
                Text(
                    text = if (scanResults.isEmpty()) "Press scan to discover networks"
                    else "${scanResults.size} networks found",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { showSpectrum = true },
                modifier = androidx.compose.ui.Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBlue.copy(alpha = 0.10f))
                    .border(BorderStroke(1.dp, CyberBlue.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.BarChart, null, tint = CyberBlue)
            }
            Button(
                onClick = { viewModel.startNetworkScan() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen.copy(alpha = 0.15f),
                    contentColor = CyberGreen,
                    disabledContainerColor = SurfaceVariantDark,
                    disabledContentColor = TextDim
                ),
                border = BorderStroke(1.dp, if (!isScanning) CyberGreen.copy(alpha = 0.5f) else TextDim.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyberGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("SCANNING...")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SCAN")
                }
            }
            } // end Row
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (scanResults.isEmpty() && !isScanning) {
            EmptyScanState()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scanResults, key = { it.bssid }) { network ->
                    NetworkItem(
                        network = network,
                        isCurrentNetwork = network.bssid == currentInfo.bssid
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkItem(network: ScannedNetwork, isCurrentNetwork: Boolean) {
    val gradeColor = when (network.securityGrade) {
        SecurityGrade.SECURE -> CyberGreen
        SecurityGrade.GOOD -> CyberBlue
        SecurityGrade.FAIR -> CyberYellow
        SecurityGrade.POOR -> CyberOrange
        SecurityGrade.OPEN -> CyberRed
    }

    val signalPercent = ((network.rssi + 100) * 100 / 70).coerceIn(0, 100)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrentNetwork) CyberGreen.copy(alpha = 0.05f) else SurfaceDark)
            .border(
                1.dp,
                if (isCurrentNetwork) CyberGreen.copy(alpha = 0.4f) else CardBorderDark,
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Icon + SSID
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = if (network.isHidden) Icons.Default.VisibilityOff else Icons.Default.Wifi,
                contentDescription = null,
                tint = gradeColor,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = network.ssid,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (isCurrentNetwork) {
                        Text(
                            text = "●",
                            color = CyberGreen,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        )
                    }
                }
                Text(
                    text = "${network.bssid}  •  Ch ${network.channel}  •  ${network.band}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
        }

        // Right: signal + security
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SecurityBadge(label = network.securityGrade.label, color = gradeColor)
            Text(
                text = "${network.rssi} dBm",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            LinearProgressIndicator(
                progress = { signalPercent / 100f },
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = gradeColor,
                trackColor = SurfaceVariantDark
            )
        }
    }
}

@Composable
private fun SecurityBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun EmptyScanState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiFind,
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(64.dp)
            )
            Text("No scan data", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Text("Tap SCAN to discover nearby networks", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
    }
}
