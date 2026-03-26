package com.eagleeye.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleeye.data.GeoPoint
import com.eagleeye.modules.tools.ToolsViewModel
import com.eagleeye.ui.theme.*

// Simplified continent polygons as (lat, lon) pairs
private val CONTINENTS: List<List<Pair<Float, Float>>> = listOf(
    // North America
    listOf(70f to -140f, 73f to -105f, 68f to -78f, 60f to -65f, 47f to -53f,
        25f to -77f, 10f to -84f, 15f to -90f, 20f to -97f, 30f to -110f,
        32f to -117f, 48f to -124f, 60f to -130f, 70f to -140f),
    // South America
    listOf(12f to -72f, 8f to -62f, 3f to -51f, -5f to -35f, -15f to -39f,
        -23f to -43f, -34f to -53f, -55f to -65f, -52f to -70f, -43f to -73f,
        -30f to -71f, -18f to -70f, -5f to -81f, 5f to -77f, 12f to -72f),
    // Europe
    listOf(71f to 28f, 70f to 20f, 60f to 5f, 51f to 2f, 43f to -8f,
        36f to -5f, 36f to 14f, 40f to 26f, 42f to 35f, 48f to 40f,
        55f to 38f, 60f to 30f, 65f to 26f, 71f to 28f),
    // Africa
    listOf(37f to 10f, 37f to 37f, 12f to 44f, -12f to 40f, -35f to 20f,
        -35f to 17f, -22f to 13f, -5f to 10f, 5f to 2f, 15f to -17f,
        20f to -17f, 37f to 10f),
    // Asia
    listOf(70f to 30f, 70f to 140f, 55f to 135f, 43f to 132f, 35f to 120f,
        22f to 114f, 5f to 103f, -8f to 115f, 0f to 130f, 5f to 105f,
        22f to 90f, 26f to 80f, 38f to 56f, 42f to 50f, 55f to 60f,
        65f to 55f, 70f to 30f),
    // Australia
    listOf(-17f to 122f, -15f to 130f, -17f to 140f, -27f to 153f,
        -38f to 147f, -38f to 140f, -35f to 136f, -32f to 131f, -17f to 122f),
    // Greenland
    listOf(83f to -45f, 76f to -18f, 70f to -24f, 65f to -38f,
        70f to -52f, 76f to -68f, 83f to -45f)
)

private fun countryColor(code: String): Color = when (code.uppercase()) {
    "US", "CA" -> Color(0xFF4A90D9)
    "CN", "JP", "KR" -> Color(0xFFFF3B5C)
    "DE", "FR", "NL", "GB", "SE", "NO", "FI" -> Color(0xFFFFD700)
    "RU" -> Color(0xFFFF6B35)
    "AU", "NZ" -> Color(0xFF00C8FF)
    "BR", "AR" -> Color(0xFF00E676)
    "IN" -> Color(0xFFFF9500)
    else -> Color(0xFFBB86FC)
}

@Composable
fun GeoMapScreen(
    toolsViewModel: ToolsViewModel,
    onBack: () -> Unit
) {
    val home by toolsViewModel.geoHome.collectAsState()
    val points by toolsViewModel.geoPoints.collectAsState()
    val isRunning by toolsViewModel.geoRunning.collectAsState()
    val geoError by toolsViewModel.geoError.collectAsState()
    var addIpText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (home == null && !isRunning) toolsViewModel.loadGeoMap()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "geo")
    val arcFlow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "arc"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = CyberGreen)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "GEO MAP",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = CyberGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                } else {
                    IconButton(onClick = { toolsViewModel.loadGeoMap() }) {
                        Icon(Icons.Default.Refresh, null, tint = TextDim, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── World map canvas ───────────────────────────────────────────────
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val w = size.width
                val h = size.height

                fun proj(lat: Float, lon: Float) =
                    Offset((lon + 180f) / 360f * w, (90f - lat) / 180f * h)

                // Grid
                val gridColor = Color(0xFF00FF88).copy(alpha = 0.05f)
                for (latDeg in -90..90 step 30) {
                    val y = (90f - latDeg) / 180f * h
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
                }
                for (lonDeg in -180..180 step 30) {
                    val x = (lonDeg + 180f) / 360f * w
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), 0.5.dp.toPx())
                }

                // Equator highlight
                val eqY = 90f / 180f * h
                drawLine(Color(0xFF00FF88).copy(alpha = 0.08f), Offset(0f, eqY), Offset(w, eqY), 0.8.dp.toPx())

                // Continents
                CONTINENTS.forEach { polygon ->
                    val path = Path()
                    polygon.forEachIndexed { i, (lat, lon) ->
                        val p = proj(lat, lon)
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    path.close()
                    drawPath(path, color = Color(0xFF00FF88).copy(alpha = 0.06f))
                    drawPath(path, color = Color(0xFF00FF88).copy(alpha = 0.20f),
                        style = Stroke(0.6.dp.toPx()))
                }

                // Arcs + blips to each destination
                val homePos = home?.let { proj(it.lat, it.lon) }
                if (homePos != null && points.isNotEmpty()) {
                    points.forEachIndexed { idx, point ->
                        val destPos = proj(point.lat, point.lon)
                        val dotColor = countryColor(point.countryCode)

                        // Control point: mid-arc, bowed above (toward North Pole)
                        val ctrl = Offset(
                            (homePos.x + destPos.x) / 2f,
                            minOf(homePos.y, destPos.y) - h * 0.13f
                        )

                        // Static dim arc
                        val arcPath = Path().apply {
                            moveTo(homePos.x, homePos.y)
                            quadraticTo(ctrl.x, ctrl.y, destPos.x, destPos.y)
                        }
                        drawPath(arcPath, dotColor.copy(alpha = 0.18f),
                            style = Stroke(0.8.dp.toPx()))

                        // Traveling blip — different phase per destination
                        val phase = (arcFlow + idx * 0.28f) % 1f
                        val blip = quadBez(phase, homePos, ctrl, destPos)
                        drawCircle(dotColor.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = blip)
                        drawCircle(dotColor, radius = 2.5.dp.toPx(), center = blip)

                        // Destination dot with pulse ring
                        val pr = 5.dp.toPx() + pulse * 7.dp.toPx()
                        drawCircle(dotColor.copy(alpha = (1f - pulse) * 0.35f), radius = pr, center = destPos)
                        drawCircle(dotColor.copy(alpha = 0.22f), radius = 5.dp.toPx(), center = destPos)
                        drawCircle(dotColor, radius = 3.5.dp.toPx(), center = destPos,
                            style = Stroke(1.5.dp.toPx()))
                    }
                }

                // Home dot
                if (homePos != null) {
                    drawCircle(CyberGreen.copy(alpha = 0.15f + pulse * 0.15f),
                        radius = 12.dp.toPx() + pulse * 4.dp.toPx(), center = homePos)
                    drawCircle(CyberGreen.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = homePos)
                    drawCircle(CyberGreen, radius = 4.dp.toPx(), center = homePos)
                }

                // Labels
                if (homePos != null) {
                    val labelPaint = android.graphics.Paint().apply {
                        textSize = 8.sp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    points.forEach { point ->
                        val pos = proj(point.lat, point.lon)
                        val label = point.city.take(8).ifBlank { point.countryCode }
                        labelPaint.color = android.graphics.Color.argb(170, 200, 200, 255)
                        drawContext.canvas.nativeCanvas.drawText(label, pos.x, pos.y - 9.dp.toPx(), labelPaint)
                    }
                    labelPaint.color = android.graphics.Color.argb(210, 0, 255, 136)
                    drawContext.canvas.nativeCanvas.drawText("YOU", homePos.x, homePos.y - 11.dp.toPx(), labelPaint)
                }

                // Loading hint / error
                if (homePos == null && !isRunning) {
                    val hintPaint = android.graphics.Paint().apply {
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    if (geoError != null) {
                        hintPaint.color = android.graphics.Color.argb(200, 255, 80, 80)
                        drawContext.canvas.nativeCanvas.drawText(geoError!!, w / 2, h / 2 - 14.sp.toPx(), hintPaint)
                        hintPaint.color = android.graphics.Color.argb(120, 200, 200, 200)
                        hintPaint.textSize = 10.sp.toPx()
                        drawContext.canvas.nativeCanvas.drawText("Tap ↻ to retry", w / 2, h / 2 + 4.sp.toPx(), hintPaint)
                    } else {
                        hintPaint.color = android.graphics.Color.argb(100, 0, 255, 136)
                        drawContext.canvas.nativeCanvas.drawText("Tap ↻ to load map", w / 2, h / 2, hintPaint)
                    }
                }
            }

            // ── Bottom panel ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1525))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add IP row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = addIpText,
                        onValueChange = { addIpText = it },
                        placeholder = {
                            Text("Add IP to map...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDim)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen.copy(alpha = 0.6f),
                            unfocusedBorderColor = CardBorderDark,
                            cursorColor = CyberGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            val ip = addIpText.trim()
                            if (ip.isNotEmpty()) {
                                toolsViewModel.addGeoIp(ip)
                                addIpText = ""
                            }
                        },
                        enabled = addIpText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen.copy(alpha = 0.15f),
                            contentColor = CyberGreen,
                            disabledContainerColor = SurfaceVariantDark,
                            disabledContentColor = TextDim
                        ),
                        border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("ADD", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Chips for home + destinations
                if (home != null || points.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        home?.let { GeoChip(it, isHome = true) }
                        points.forEach { GeoChip(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeoChip(point: GeoPoint, isHome: Boolean = false) {
    val color = if (isHome) CyberGreen else countryColor(point.countryCode)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                text = if (isHome) "YOU · ${point.ip}" else "${point.countryCode} · ${point.ip}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = color
            )
            Text(
                text = point.city.ifBlank { point.country }.take(22),
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }
}

/** Quadratic Bézier point at parameter t (0..1). */
private fun quadBez(t: Float, p0: Offset, p1: Offset, p2: Offset): Offset {
    val mt = 1f - t
    return Offset(
        mt * mt * p0.x + 2f * mt * t * p1.x + t * t * p2.x,
        mt * mt * p0.y + 2f * mt * t * p1.y + t * t * p2.y
    )
}
