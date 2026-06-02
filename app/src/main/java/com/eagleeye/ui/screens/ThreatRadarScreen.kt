package com.eagleeye.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleeye.data.LanDevice
import com.eagleeye.data.Threat
import com.eagleeye.data.ThreatLevel
import com.eagleeye.ui.theme.*
import kotlin.math.*

@Composable
fun ThreatRadarScreen(
    devices: List<LanDevice>,
    threats: List<Threat>,
    onBack: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "sweep"
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse"
    )

    val criticalCount = threats.count { it.level == ThreatLevel.CRITICAL || it.level == ThreatLevel.HIGH }

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
                    "THREAT RADAR",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${devices.size} nodes",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            // Radar canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxRadius = minOf(cx, cy) * 0.88f

                // ── Background rings ──────────────────────────────────────
                for (i in 1..3) {
                    val r = maxRadius * i / 3f
                    drawCircle(
                        color = Color(0xFF00FF88).copy(alpha = 0.10f),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // ── Crosshair ─────────────────────────────────────────────
                val lineColor = Color(0xFF00FF88).copy(alpha = 0.12f)
                drawLine(lineColor, Offset(cx - maxRadius, cy), Offset(cx + maxRadius, cy), 1.dp.toPx())
                drawLine(lineColor, Offset(cx, cy - maxRadius), Offset(cx, cy + maxRadius), 1.dp.toPx())
                drawLine(lineColor, Offset(cx - maxRadius * 0.707f, cy - maxRadius * 0.707f),
                    Offset(cx + maxRadius * 0.707f, cy + maxRadius * 0.707f), 0.5.dp.toPx())
                drawLine(lineColor, Offset(cx + maxRadius * 0.707f, cy - maxRadius * 0.707f),
                    Offset(cx - maxRadius * 0.707f, cy + maxRadius * 0.707f), 0.5.dp.toPx())

                // ── Sweep wedge ───────────────────────────────────────────
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.13f to Color(0xFF00FF88).copy(alpha = 0.18f),
                        0.167f to Color.Transparent,
                        center = Offset(cx, cy)
                    ),
                    startAngle = sweepAngle - 60f,
                    sweepAngle = 60f,
                    useCenter = true,
                    topLeft = Offset(cx - maxRadius, cy - maxRadius),
                    size = Size(maxRadius * 2, maxRadius * 2)
                )

                // Sweep leading line
                val sweepRad = Math.toRadians(sweepAngle.toDouble())
                drawLine(
                    color = Color(0xFF00FF88).copy(alpha = 0.75f),
                    start = Offset(cx, cy),
                    end = Offset(cx + maxRadius * cos(sweepRad).toFloat(), cy + maxRadius * sin(sweepRad).toFloat()),
                    strokeWidth = 2.dp.toPx()
                )

                // ── Center dot (your device) ──────────────────────────────
                drawCircle(Color(0xFF00FF88), radius = 10.dp.toPx(), center = Offset(cx, cy))
                drawCircle(Color(0xFF00FF88).copy(alpha = 0.2f), radius = 18.dp.toPx(), center = Offset(cx, cy),
                    style = Stroke(1.5.dp.toPx()))

                // ── Device dots ───────────────────────────────────────────
                devices.forEachIndexed { idx, device ->
                    val macHash = device.mac.hashCode()
                    val angle = (macHash.toLong() and 0xFFFFFFFFL).toDouble() / 0xFFFFFFFFL * 2 * PI
                    val riskFraction = when {
                        !device.isOnline -> 0.82f
                        device.isKnown && (device.openPortList.size <= 2) -> 0.62f + (idx % 5) * 0.03f
                        device.isKnown -> 0.52f + (idx % 4) * 0.03f
                        else -> 0.28f + (idx % 5) * 0.04f
                    }
                    val dist = maxRadius * riskFraction
                    val dx = cx + dist * cos(angle).toFloat()
                    val dy = cy + dist * sin(angle).toFloat()
                    val pos = Offset(dx, dy)

                    val dotColor = when {
                        !device.isOnline -> Color(0xFF4A6080)
                        device.isKnown -> Color(0xFF00FF88)
                        device.openPortList.size > 3 -> Color(0xFFFF3B5C)
                        else -> Color(0xFFFF9500)
                    }
                    val dotRadius = if (!device.isKnown && device.isOnline) 9.dp.toPx() else 7.dp.toPx()

                    // Pulse ring for unknown/high-risk online devices
                    if (device.isOnline && (!device.isKnown || device.openPortList.size > 3)) {
                        drawCircle(
                            color = dotColor.copy(alpha = (1f - pulseProgress) * 0.5f),
                            radius = dotRadius + pulseProgress * 22.dp.toPx(),
                            center = pos,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Dot fill
                    drawCircle(dotColor.copy(alpha = 0.25f), radius = dotRadius, center = pos)
                    drawCircle(dotColor, radius = dotRadius, center = pos, style = Stroke(2.dp.toPx()))

                    // Label
                    val label = device.displayName.take(10).ifBlank { device.ip.substringAfterLast('.').let { ".$it" } }
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(180, 255, 255, 255)
                        textSize = 9.sp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(label, dx, dy + dotRadius + 12.dp.toPx(), paint)
                }

                // ── Active threat markers (inner warning ring) ────────────
                threats.filter { it.level == ThreatLevel.CRITICAL || it.level == ThreatLevel.HIGH }
                    .forEachIndexed { i, _ ->
                        val a = i * 2 * PI / maxOf(1, criticalCount) - PI / 2
                        val r2 = maxRadius * 0.18f
                        val tx = cx + r2 * cos(a).toFloat()
                        val ty = cy + r2 * sin(a).toFloat()
                        drawCircle(Color(0xFFFF3B5C).copy(alpha = 0.4f + pulseProgress * 0.3f),
                            radius = 6.dp.toPx(), center = Offset(tx, ty))
                        drawCircle(Color(0xFFFF3B5C), radius = 4.dp.toPx(),
                            center = Offset(tx, ty), style = Stroke(1.5.dp.toPx()))
                    }
            }

            // ── Bottom legend + threat summary ─────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1525))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (criticalCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberRed.copy(alpha = 0.1f))
                            .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                        Text(
                            "$criticalCount ACTIVE THREAT${if (criticalCount > 1) "S" else ""} DETECTED",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendDot("KNOWN", CyberGreen)
                    LegendDot("UNKNOWN", CyberOrange)
                    LegendDot("HIGH RISK", CyberRed)
                    LegendDot("OFFLINE", Color(0xFF4A6080))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CENTER = you  ·  INNER RING = high risk", style = MaterialTheme.typography.labelMedium, color = TextDim)
                    Text("${devices.count { it.isOnline }} online", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color.copy(alpha = 0.3f))
            drawCircle(color, style = Stroke(1.5.dp.toPx()))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
    }
}
