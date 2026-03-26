@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.eagleeye.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import com.eagleeye.data.SignalSample
import com.eagleeye.data.SignalStrength
import com.eagleeye.data.WifiConnectionInfo
import com.eagleeye.modules.wifi.WifiViewModel
import com.eagleeye.modules.wifi.WifiQrGenerator
import com.eagleeye.modules.tools.ToolsViewModel
import com.eagleeye.ui.theme.*

@Composable
fun DashboardScreen(viewModel: WifiViewModel, toolsViewModel: ToolsViewModel) {
    val info by viewModel.connectionInfo.collectAsState()
    val signalHistory by viewModel.signalHistory.collectAsState()
    var showGeoMap by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    if (showGeoMap) {
        GeoMapScreen(toolsViewModel = toolsViewModel, onBack = { showGeoMap = false })
        return
    }

    if (showQr && info.isConnected) {
        WifiQrDialog(info = info, onDismiss = { showQr = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EAGLEEYE",
                    style = MaterialTheme.typography.displaySmall,
                    color = CyberGreen
                )
                Text(
                    text = "Network Intelligence",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (info.isConnected) {
                    IconButton(
                        onClick = { showQr = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberGreen.copy(alpha = 0.10f))
                            .border(BorderStroke(1.dp, CyberGreen.copy(alpha = 0.30f)), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = CyberGreen)
                    }
                }
                IconButton(
                    onClick = { showGeoMap = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBlue.copy(alpha = 0.10f))
                        .border(BorderStroke(1.dp, CyberBlue.copy(alpha = 0.30f)), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Map, contentDescription = "GeoMap", tint = CyberBlue)
                }
                ConnectionStatusBadge(isConnected = info.isConnected)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (info.isConnected) {
            // Signal + SSID card
            SignalCard(info)

            // IP Info card
            InfoCard(title = "NETWORK") {
                InfoRow(Icons.Default.Router, "Gateway", info.gateway)
                InfoRow(Icons.Default.DeviceHub, "Local IP", info.ipAddress)
                InfoRow(Icons.Default.Lan, "Subnet", info.subnetMask)
                InfoRow(Icons.Default.Dns, "DNS Primary", info.dns1)
                if (info.dns2.isNotEmpty() && info.dns2 != "0.0.0.0") {
                    InfoRow(Icons.Default.Dns, "DNS Secondary", info.dns2)
                }
            }

            // Signal history
            if (signalHistory.size >= 3) {
                SignalHistoryCard(history = signalHistory)
            }

            // Connection quality card
            InfoCard(title = "CONNECTION") {
                InfoRow(Icons.Default.Speed, "Link Speed", "${info.linkSpeedMbps} Mbps")
                InfoRow(Icons.Default.Wifi, "Frequency", "${info.frequencyMhz} MHz")
                InfoRow(Icons.Default.CellTower, "Band", info.band)
                InfoRow(Icons.Default.Router, "BSSID", info.bssid)
                if (info.securityType.isNotEmpty()) {
                    InfoRow(Icons.Default.Lock, "Security", info.securityType)
                }
            }
        } else {
            NotConnectedCard()
        }
    }
}

@Composable
private fun SignalCard(info: WifiConnectionInfo) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val signalColor = when (info.signalStrength) {
        SignalStrength.EXCELLENT -> CyberGreen
        SignalStrength.GOOD -> CyberGreen
        SignalStrength.FAIR -> CyberYellow
        SignalStrength.WEAK -> CyberOrange
        SignalStrength.NONE -> CyberRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CardDark, SurfaceVariantDark),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(1.dp, signalColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.ssid,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = info.signalStrength.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = signalColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                SignalBar(percent = info.signalPercent, color = signalColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${info.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Signal rings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp)
            ) {
                repeat(info.signalStrength.bars) { i ->
                    val size = (32 + i * 12).dp
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(signalColor.copy(alpha = (0.05f + i * 0.05f) * pulseAlpha))
                    )
                }
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SignalBar(percent: Int, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(SurfaceVariantDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent / 100f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.5f), color)
                    )
                )
        )
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = CyberGreen
        )
        HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
        content()
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(label, value))
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied: $label", Toast.LENGTH_SHORT).show()
                }
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ConnectionStatusBadge(isConnected: Boolean) {
    val color = if (isConnected) CyberGreen else CyberRed
    val text = if (isConnected) "ONLINE" else "OFFLINE"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

// ── Wi-Fi QR Dialog ───────────────────────────────────────────────────────────

@Composable
private fun WifiQrDialog(info: WifiConnectionInfo, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    val qrBitmap = remember(password, info.ssid, info.securityType) {
        runCatching { WifiQrGenerator.generate(info.ssid, password, info.securityType) }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.QrCode, null, tint = CyberGreen, modifier = Modifier.size(20.dp))
                Text("Wi-Fi QR Code", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // QR preview
                qrBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.ui.graphics.Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Wi-Fi QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Network info
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(info.ssid, style = MaterialTheme.typography.bodyMedium,
                        color = CyberGreen, fontWeight = FontWeight.Bold)
                    Text(info.securityType.ifBlank { "Open" },
                        style = MaterialTheme.typography.bodySmall, color = TextDim)
                }

                // Password input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("Enter to include in QR", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGreen.copy(alpha = 0.6f),
                        unfocusedBorderColor = CardBorderDark,
                        focusedLabelColor = CyberGreen,
                        unfocusedLabelColor = TextDim,
                        cursorColor = CyberGreen,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Text(
                    "Android can't read the saved password. Enter it to create a full QR.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyberGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SignalHistoryCard(history: List<SignalSample>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SIGNAL HISTORY", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
            Text(
                "${history.last().rssi} dBm",
                style = MaterialTheme.typography.labelMedium,
                color = TextDim
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val minRssi = history.minOf { it.rssi }.toFloat()
        val maxRssi = history.maxOf { it.rssi }.toFloat()
        val range = (maxRssi - minRssi).coerceAtLeast(10f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            val w = size.width
            val h = size.height
            val step = if (history.size > 1) w / (history.size - 1) else w

            // Fill path
            val fillPath = Path().apply {
                moveTo(0f, h)
                history.forEachIndexed { i, sample ->
                    val x = i * step
                    val y = h - ((sample.rssi - minRssi) / range) * h
                    if (i == 0) lineTo(x, y) else lineTo(x, y)
                }
                lineTo((history.size - 1) * step, h)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    listOf(CyberGreen.copy(alpha = 0.25f), CyberGreen.copy(alpha = 0.02f))
                )
            )

            // Line path
            val linePath = Path().apply {
                history.forEachIndexed { i, sample ->
                    val x = i * step
                    val y = h - ((sample.rssi - minRssi) / range) * h
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(linePath, color = CyberGreen, style = Stroke(width = 2.dp.toPx()))

            // Current dot
            val lastX = (history.size - 1) * step
            val lastY = h - ((history.last().rssi - minRssi) / range) * h
            drawCircle(CyberGreen, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${history.size} samples",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
            Text(
                "min ${history.minOf { it.rssi }} / max ${history.maxOf { it.rssi }} dBm",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }
}

@Composable
private fun NotConnectedCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = CyberRed,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "NOT CONNECTED",
                style = MaterialTheme.typography.titleMedium,
                color = CyberRed
            )
            Text(
                text = "Connect to a Wi-Fi network to begin analysis",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
        }
    }
}
