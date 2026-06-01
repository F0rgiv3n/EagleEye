@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.eagleeye.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleeye.data.LanDevice
import com.eagleeye.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun TopologyScreen(
    devices: List<LanDevice>,
    gatewayIp: String,
    onBack: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedDevice by remember { mutableStateOf<LanDevice?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offset += panChange
    }

    // Pre-compute positions when canvas size or devices change
    val positions by remember(devices, canvasSize, gatewayIp) {
        derivedStateOf { nodePositions(devices, canvasSize, gatewayIp) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberGreen)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "TOPOLOGY MAP",
                color = CyberGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        // Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(state = transformableState)
                .pointerInput(positions, scale, offset) {
                    detectTapGestures { tapOffset ->
                        // Adjust tap for current transform
                        val adjustedX = (tapOffset.x - offset.x) / scale
                        val adjustedY = (tapOffset.y - offset.y) / scale
                        val adjusted = Offset(adjustedX, adjustedY)

                        val hitRadius = 60f / scale
                        val tapped = positions.entries.firstOrNull { (_, pos) ->
                            val dx = pos.x - adjusted.x
                            val dy = pos.y - adjusted.y
                            sqrt(dx * dx + dy * dy) < hitRadius
                        }
                        selectedDevice = if (tapped != null) {
                            val ip = tapped.key
                            devices.find { it.ip == ip }
                                ?: if (ip == gatewayIp) LanDevice(
                                    mac = "",
                                    ip = gatewayIp,
                                    hostname = "Gateway",
                                    vendor = "",
                                    isOnline = true,
                                    latencyMs = 0,
                                    openPorts = "",
                                    firstSeen = 0,
                                    lastSeen = 0,
                                    isKnown = true
                                ) else null
                        } else null
                    }
                }
        ) {
            canvasSize = this.size

            if (positions.isEmpty()) return@Canvas

            val gatewayPos = positions[gatewayIp] ?: Offset(size.width / 2, size.height / 2)

            // Draw lines first
            positions.forEach { (ip, pos) ->
                if (ip == gatewayIp) return@forEach
                val device = devices.find { it.ip == ip }
                val lineColor = when {
                    device == null -> TextDim
                    !device.isOnline -> TextDim
                    !device.isKnown -> CyberOrange
                    else -> CyberGreen
                }.copy(alpha = 0.35f)
                drawLine(
                    color = lineColor,
                    start = gatewayPos,
                    end = pos,
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Draw gateway node
            val gwRadius = 28.dp.toPx()
            val isGwSelected = selectedDevice?.ip == gatewayIp
            if (isGwSelected) {
                drawCircle(
                    color = CyberGreen.copy(alpha = 0.4f),
                    radius = gwRadius + 6.dp.toPx(),
                    center = gatewayPos
                )
                drawCircle(
                    color = CyberGreen.copy(alpha = 0.2f),
                    radius = gwRadius + 12.dp.toPx(),
                    center = gatewayPos
                )
            }
            drawCircle(
                color = CyberGreen.copy(alpha = 0.2f),
                radius = gwRadius,
                center = gatewayPos
            )
            drawCircle(
                color = CyberGreen,
                radius = gwRadius,
                center = gatewayPos,
                style = Stroke(width = 2.dp.toPx())
            )

            // Gateway label
            val gwPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 10.sp.toPx()
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "GATEWAY",
                gatewayPos.x,
                gatewayPos.y - gwRadius - 10.dp.toPx(),
                gwPaint
            )
            val gwIpPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(180, 0, 255, 136)
                textSize = 9.sp.toPx()
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                gatewayIp,
                gatewayPos.x,
                gatewayPos.y + gwRadius + 16.dp.toPx(),
                gwIpPaint
            )

            // Draw device nodes
            devices.forEach { device ->
                val pos = positions[device.ip] ?: return@forEach
                drawDeviceNode(device, pos, device.ip == selectedDevice?.ip)
            }
        }

        // Legend
        LegendOverlay(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = if (selectedDevice != null) 200.dp else 24.dp)
        )

        // Selected device info card
        AnimatedVisibility(
            visible = selectedDevice != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedDevice?.let { device ->
                DeviceInfoCard(
                    device = device,
                    isGateway = device.ip == gatewayIp,
                    onDismiss = { selectedDevice = null }
                )
            }
        }
    }
}

private fun DrawScope.drawDeviceNode(device: LanDevice, pos: Offset, isSelected: Boolean) {
    val nodeRadius = 20.dp.toPx()

    val (fillColor, borderColor) = when {
        !device.isOnline -> Color(0xFF2A2A2A) to Color(0xFF4A6080)
        !device.isKnown -> CyberOrange.copy(alpha = 0.6f) to CyberOrange
        else -> CyberGreen.copy(alpha = 0.3f) to CyberGreen
    }

    // Glow for selected
    if (isSelected) {
        drawCircle(
            color = borderColor.copy(alpha = 0.4f),
            radius = nodeRadius + 6.dp.toPx(),
            center = pos
        )
        drawCircle(
            color = borderColor.copy(alpha = 0.2f),
            radius = nodeRadius + 12.dp.toPx(),
            center = pos
        )
    }

    drawCircle(color = fillColor, radius = nodeRadius, center = pos)
    drawCircle(
        color = borderColor,
        radius = nodeRadius,
        center = pos,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Risk dot (red) if open ports > 3
    if (device.openPortList.size > 3) {
        val dotOffset = Offset(
            pos.x + nodeRadius * 0.7f,
            pos.y - nodeRadius * 0.7f
        )
        drawCircle(color = Color(0xFFFF3B5C), radius = 5.dp.toPx(), center = dotOffset)
    }

    // Label
    val shortName = when {
        device.hostname.isNotBlank() && device.hostname != device.ip ->
            device.hostname.take(10)
        else -> ".${device.ip.substringAfterLast(".")}"
    }
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 10.sp.toPx()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        shortName,
        pos.x,
        pos.y + nodeRadius + 14.dp.toPx(),
        labelPaint
    )
}

fun nodePositions(
    devices: List<LanDevice>,
    canvasSize: Size,
    gatewayIp: String
): Map<String, Offset> {
    if (canvasSize.width == 0f || canvasSize.height == 0f) return emptyMap()

    val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
    val online = devices.filter { it.isOnline && it.ip != gatewayIp }
    val offline = devices.filter { !it.isOnline }
    val positions = mutableMapOf<String, Offset>()

    positions[gatewayIp] = center

    fun placeRing(list: List<LanDevice>, radiusPx: Float) {
        if (list.isEmpty()) return
        list.forEachIndexed { i, device ->
            val angle = (2 * Math.PI * i / list.size).toFloat()
            positions[device.ip] = Offset(
                center.x + radiusPx * cos(angle),
                center.y + radiusPx * sin(angle)
            )
        }
    }

    val ring1 = online.take(8)
    val ring2 = online.drop(8)
    placeRing(ring1, 380f)
    placeRing(ring2, 580f)
    placeRing(offline, 730f)

    return positions
}

@Composable
private fun DeviceInfoCard(
    device: LanDevice,
    isGateway: Boolean,
    onDismiss: () -> Unit
) {
    val statusColor = when {
        isGateway -> CyberGreen
        !device.isOnline -> Color(0xFF4A6080)
        !device.isKnown -> CyberOrange
        else -> CyberGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = if (isGateway) "GATEWAY" else if (device.isOnline) "ONLINE" else "OFFLINE",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextDim, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            CardDetailRow("IP", device.ip)
            if (device.mac.isNotBlank()) CardDetailRow("MAC", device.mac)
            if (device.vendor.isNotBlank()) CardDetailRow("Vendor", device.vendor)
            if (device.hostname.isNotBlank() && device.hostname != device.ip) {
                CardDetailRow("Hostname", device.hostname)
            }
            if (device.openPortList.isNotEmpty()) {
                CardDetailRow("Open Ports", device.openPortList.take(6).joinToString(", ") +
                    if (device.openPortList.size > 6) " +" else "")
            }
            if (!device.isKnown && !isGateway) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "NEW / UNRECOGNIZED DEVICE",
                    color = CyberOrange,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CardDetailRow(label: String, value: String) {
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
        Text(label, color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(
            value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LegendOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LegendItem(color = CyberGreen, label = "Online Known")
        LegendItem(color = CyberOrange, label = "Online New")
        LegendItem(color = Color(0xFF4A6080), label = "Offline")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
