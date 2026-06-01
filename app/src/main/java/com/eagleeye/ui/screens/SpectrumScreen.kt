package com.eagleeye.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleeye.data.ScannedNetwork
import com.eagleeye.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

private val AP_COLORS = listOf(
    Color(0xFF00FF88), // CyberGreen
    Color(0xFF00D4FF), // CyberBlue
    Color(0xFFFF9500), // CyberOrange
    Color(0xFFFFD60A), // CyberYellow
    Color(0xFFFF3B5C), // CyberRed
    Color(0xFFBF5AF2), // Purple
    Color(0xFFFF6B9D), // Pink
    Color(0xFF00FFD4), // Cyan
)

@Composable
fun SpectrumScreen(
    networks: List<ScannedNetwork>,
    currentSsid: String,
    onBack: () -> Unit
) {
    var show5g by remember { mutableStateOf(false) }

    val visibleNetworks = remember(networks, show5g) {
        networks.filter { n ->
            if (show5g) n.frequencyMhz in 5000..6000
            else n.frequencyMhz in 2400..2500
        }.sortedBy { it.channel }
    }

    // Animated RSSI heights per BSSID
    val animatables = remember { mutableMapOf<String, Animatable<Float, AnimationVector1D>>() }
    visibleNetworks.forEach { net ->
        val anim = animatables.getOrPut(net.bssid) { Animatable(net.rssi.toFloat()) }
        LaunchedEffect(net.rssi) {
            anim.animateTo(net.rssi.toFloat(), animationSpec = spring(stiffness = Spring.StiffnessLow))
        }
    }

    // Color map — stable per BSSID
    val colorMap = remember(networks) {
        val m = mutableMapOf<String, Color>()
        networks.forEachIndexed { i, n -> m[n.bssid] = AP_COLORS[i % AP_COLORS.size] }
        m
    }

    val bestChannel = remember(visibleNetworks, show5g) {
        if (show5g) null else findBestChannel24(visibleNetworks)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CyberGreen)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "WI-FI SPECTRUM",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${visibleNetworks.size} APs",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            // Band toggle
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to "2.4 GHz", true to "5 GHz").forEach { (is5g, label) ->
                    val selected = show5g == is5g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) CyberGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (selected) CyberGreen.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { show5g = is5g }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) CyberGreen else TextDim
                        )
                    }
                }
            }

            // Spectrum Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val w = size.width
                val h = size.height
                val padLeft = 36.dp.toPx()
                val padRight = 12.dp.toPx()
                val padTop = 12.dp.toPx()
                val padBottom = 28.dp.toPx()
                val plotW = w - padLeft - padRight
                val plotH = h - padTop - padBottom
                val rssiMin = -100f
                val rssiMax = -20f

                // Y axis grid lines + labels
                listOf(-30f, -50f, -70f, -90f).forEach { rssi ->
                    val yFrac = (rssi - rssiMax) / (rssiMin - rssiMax)
                    val yPx = padTop + yFrac * plotH
                    drawLine(Color(0xFF1A2744), Offset(padLeft, yPx), Offset(w - padRight, yPx), 0.8.dp.toPx())
                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(140, 138, 163, 193)
                        textSize = 8.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${rssi.toInt()}", padLeft - 4.dp.toPx(), yPx + 3.dp.toPx(), labelPaint
                    )
                }

                if (!show5g) {
                    // ── 2.4 GHz channels 1-13 ──
                    val channels = 1..13
                    val chW = plotW / (channels.last - channels.first).toFloat()

                    // Channel axis labels
                    val chPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(150, 138, 163, 193)
                        textSize = 8.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    channels.forEach { ch ->
                        val xPx = padLeft + (ch - 1f) / (channels.last - 1f) * plotW
                        drawLine(Color(0xFF141E35), Offset(xPx, padTop), Offset(xPx, padTop + plotH), 0.5.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            "$ch", xPx, h - 4.dp.toPx(), chPaint
                        )
                    }

                    // Best channel indicator
                    bestChannel?.let { best ->
                        val bx = padLeft + (best - 1f) / (channels.last - 1f) * plotW
                        drawLine(
                            Color(0xFF00FF88).copy(alpha = 0.4f),
                            Offset(bx, padTop), Offset(bx, padTop + plotH),
                            2.dp.toPx()
                        )
                        val bestPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(220, 0, 255, 136)
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.MONOSPACE
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "▲ CH$best", bx, padTop + plotH + 20.dp.toPx(), bestPaint
                        )
                    }

                    // Mountains
                    visibleNetworks.forEach { net ->
                        val color = colorMap[net.bssid] ?: CyberGreen
                        val rssiAnim = animatables[net.bssid]?.value ?: net.rssi.toFloat()
                        val isWide = net.frequencyMhz > 2450 // rough HT40 heuristic
                        val halfWidthCh = if (isWide) 3.5f else 2f
                        val centerX = padLeft + (net.channel - 1f) / (channels.last - 1f) * plotW
                        val halfWidthPx = halfWidthCh / (channels.last - 1f) * plotW
                        val yFrac = (rssiAnim - rssiMax) / (rssiMin - rssiMax)
                        val peakY = padTop + yFrac * plotH

                        drawMountain(
                            centerX = centerX, peakY = peakY,
                            halfWidthPx = halfWidthPx, bottomY = padTop + plotH,
                            fillBrush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.04f)),
                                startY = peakY, endY = padTop + plotH
                            ),
                            strokeColor = color.copy(alpha = 0.9f)
                        )

                        // SSID label at peak
                        val ssidLabel = net.ssid.take(11).ifBlank { "Hidden" }
                        val ssidPaint = android.graphics.Paint().apply {
                            this.color = android.graphics.Color.argb(210, 255, 255, 255)
                            textSize = 8.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        if (peakY > padTop + 10.dp.toPx()) {
                            drawContext.canvas.nativeCanvas.drawText(ssidLabel, centerX, peakY - 4.dp.toPx(), ssidPaint)
                        }
                    }
                } else {
                    // ── 5 GHz ──
                    val ch5List = listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 149, 153, 157, 161, 165)
                    val chPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(120, 138, 163, 193)
                        textSize = 7.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    fun ch5ToX(ch: Int): Float {
                        val idx = ch5List.indexOf(ch).coerceAtLeast(0)
                        return padLeft + idx.toFloat() / (ch5List.size - 1) * plotW
                    }

                    ch5List.filterIndexed { i, _ -> i % 4 == 0 }.forEach { ch ->
                        val xPx = ch5ToX(ch)
                        drawLine(Color(0xFF141E35), Offset(xPx, padTop), Offset(xPx, padTop + plotH), 0.5.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText("$ch", xPx, h - 4.dp.toPx(), chPaint)
                    }

                    visibleNetworks.forEach { net ->
                        val color = colorMap[net.bssid] ?: CyberBlue
                        val rssiAnim = animatables[net.bssid]?.value ?: net.rssi.toFloat()
                        val centerX = ch5ToX(net.channel)
                        val halfWidthPx = 2.5f / ch5List.size * plotW
                        val yFrac = (rssiAnim - rssiMax) / (rssiMin - rssiMax)
                        val peakY = padTop + yFrac * plotH

                        drawMountain(
                            centerX = centerX, peakY = peakY,
                            halfWidthPx = halfWidthPx, bottomY = padTop + plotH,
                            fillBrush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.04f)),
                                startY = peakY, endY = padTop + plotH
                            ),
                            strokeColor = color.copy(alpha = 0.9f)
                        )
                        val ssidPaint = android.graphics.Paint().apply {
                            this.color = android.graphics.Color.argb(210, 255, 255, 255)
                            textSize = 8.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        if (peakY > padTop + 10.dp.toPx()) {
                            drawContext.canvas.nativeCanvas.drawText(
                                net.ssid.take(10).ifBlank { "Hidden" }, centerX, peakY - 4.dp.toPx(), ssidPaint
                            )
                        }
                    }
                }
            }

            // Best channel hint
            if (!show5g && bestChannel != null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberGreen.copy(alpha = 0.08f))
                        .border(1.dp, CyberGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = CyberGreen, modifier = Modifier.size(14.dp))
                    Text(
                        "Recommended: Channel $bestChannel (least interference)",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberGreen
                    )
                }
            }

            // Network list
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(visibleNetworks, key = { it.bssid }) { net ->
                    val color = colorMap[net.bssid] ?: CyberGreen
                    val isConnected = net.ssid == currentSsid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.dp, if (isConnected) CyberGreen.copy(0.3f) else CardBorderDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    net.ssid.ifBlank { "Hidden Network" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isConnected) {
                                    Text(
                                        "CONNECTED",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberGreen,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(CyberGreen.copy(alpha = 0.12f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                "CH ${net.channel}  ·  ${net.frequencyMhz} MHz  ·  ${net.securityType.take(12)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDim
                            )
                        }
                        // Mini RSSI bar
                        val rssiPct = ((net.rssi + 100f) / 70f).coerceIn(0f, 1f)
                        val rssiColor = when {
                            net.rssi >= -60 -> CyberGreen
                            net.rssi >= -75 -> CyberYellow
                            else -> CyberOrange
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${net.rssi} dBm", style = MaterialTheme.typography.labelMedium, color = rssiColor)
                            LinearProgressIndicator(
                                progress = { rssiPct },
                                modifier = Modifier.width(48.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                                color = rssiColor,
                                trackColor = SurfaceVariantDark
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMountain(
    centerX: Float, peakY: Float, halfWidthPx: Float,
    bottomY: Float, fillBrush: Brush, strokeColor: Color
) {
    val path = Path().apply {
        moveTo(centerX - halfWidthPx, bottomY)
        quadraticBezierTo(centerX, peakY, centerX + halfWidthPx, bottomY)
        close()
    }
    drawPath(path, brush = fillBrush)
    val strokePath = Path().apply {
        moveTo(centerX - halfWidthPx, bottomY)
        quadraticBezierTo(centerX, peakY, centerX + halfWidthPx, bottomY)
    }
    drawPath(strokePath, color = strokeColor, style = Stroke(width = 2.dp.toPx()))
}

private fun findBestChannel24(networks: List<ScannedNetwork>): Int {
    // Non-overlapping 2.4GHz channels: 1, 6, 11
    val candidates = listOf(1, 6, 11)
    // For each candidate, sum interference from nearby networks
    // A network on channel X interferes with channels X-4 to X+4
    return candidates.minByOrNull { candidate ->
        networks.sumOf { net ->
            val dist = abs(net.channel - candidate)
            if (dist <= 4) max(0, (5 - dist)) else 0
        }
    } ?: 1
}
