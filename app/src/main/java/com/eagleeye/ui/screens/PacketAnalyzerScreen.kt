package com.eagleeye.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleeye.data.CapturedPacket
import com.eagleeye.data.IpProtocol
import com.eagleeye.data.PacketStats
import com.eagleeye.modules.packet.PacketViewModel
import com.eagleeye.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PacketAnalyzerTool(packetViewModel: PacketViewModel) {
    val context = LocalContext.current
    val isCapturing by packetViewModel.isCapturing.collectAsState()
    val stats by packetViewModel.stats.collectAsState()
    val recentPackets by packetViewModel.recentPackets.collectAsState()
    val vpnIntent by packetViewModel.vpnPermissionIntent.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            packetViewModel.onVpnPermissionResult(true, context)
        }
        packetViewModel.clearVpnPermissionIntent()
    }

    LaunchedEffect(vpnIntent) {
        vpnIntent?.let {
            vpnLauncher.launch(it)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.NetworkCheck, null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                Text(
                    "PACKET ANALYZER",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Device traffic only  •  Internet paused during capture",
                style = MaterialTheme.typography.labelMedium,
                color = TextDim
            )
            Text(
                "Uses VpnService API (no root required)",
                style = MaterialTheme.typography.labelMedium,
                color = TextDim
            )
        }

        // Active capture warning
        if (isCapturing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberOrange.copy(alpha = 0.08f))
                    .border(1.dp, CyberOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Warning, null, tint = CyberOrange, modifier = Modifier.size(16.dp))
                Column {
                    Text(
                        "Internet traffic is paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Tap STOP CAPTURE to restore internet access",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberOrange.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Start / Stop button
        Button(
            onClick = {
                if (isCapturing) {
                    packetViewModel.stopCapture(context)
                } else {
                    packetViewModel.resetStats()
                    packetViewModel.startCapture(context)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCapturing) CyberRed.copy(alpha = 0.12f)
                                 else CyberGreen.copy(alpha = 0.12f),
                contentColor = if (isCapturing) CyberRed else CyberGreen
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isCapturing) CyberRed.copy(alpha = 0.5f) else CyberGreen.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = CyberRed,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("STOP CAPTURE", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("START CAPTURE", fontWeight = FontWeight.Bold)
            }
        }

        // Stats row
        PacketStatsRow(stats)

        // Protocol breakdown
        ProtocolBreakdownRow(stats)

        // Packet rate timeline
        if (recentPackets.isNotEmpty() || isCapturing) {
            PacketRateChart(recentPackets)
        }

        // Recent packets
        if (recentPackets.isNotEmpty()) {
            RecentPacketsList(recentPackets.takeLast(50).reversed())
        }

        // Top destinations
        if (stats.topDestinations.isNotEmpty()) {
            TopDestinationsCard(stats)
        }

        // DNS queries
        if (stats.dnsQueries.isNotEmpty()) {
            DnsQueriesCard(stats.dnsQueries)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PacketStatsRow(stats: PacketStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PktStatBox("PACKETS", "${stats.totalPackets}", CyberGreen, Modifier.weight(1f))
        PktStatBox("KB", "%.1f".format(stats.totalBytes / 1024.0), CyberBlue, Modifier.weight(1f))
        PktStatBox("DNS", "${stats.dnsQueries.size}", CyberYellow, Modifier.weight(1f))
    }
}

@Composable
private fun ProtocolBreakdownRow(stats: PacketStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PktStatBox("TCP", "${stats.tcpPackets}", CyberBlue, Modifier.weight(1f))
        PktStatBox("UDP", "${stats.udpPackets}", CyberGreen, Modifier.weight(1f))
        PktStatBox("ICMP", "${stats.icmpPackets}", CyberYellow, Modifier.weight(1f))
    }
}

@Composable
private fun PktStatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
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
private fun RecentPacketsList(packets: List<CapturedPacket>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "RECENT PACKETS",
            style = MaterialTheme.typography.labelMedium,
            color = TextDim,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        packets.forEach { packet ->
            PacketRow(packet)
        }
    }
}

@Composable
private fun PacketRow(packet: CapturedPacket) {
    val (protoColor, protoLabel) = when (packet.protocol) {
        IpProtocol.TCP  -> CyberBlue to "TCP"
        IpProtocol.UDP  -> CyberGreen to "UDP"
        IpProtocol.ICMP -> CyberYellow to "ICMP"
        else            -> TextDim to "OTHER"
    }

    val displayName = when {
        packet.dnsQuery.isNotEmpty() -> packet.dnsQuery
        packet.dstPort > 0 -> "${packet.dstIp}:${packet.dstPort}"
        else -> packet.dstIp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceVariantDark)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Protocol badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(protoColor.copy(alpha = 0.15f))
                .border(1.dp, protoColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                protoLabel,
                style = MaterialTheme.typography.labelMedium,
                color = protoColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        // Destination
        Text(
            displayName,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        // Service name
        if (packet.info.isNotEmpty() && packet.dnsQuery.isEmpty()) {
            Text(
                packet.info,
                style = MaterialTheme.typography.labelMedium,
                color = CyberBlue,
                fontSize = 10.sp
            )
        }

        // Size
        Text(
            "${packet.size}B",
            style = MaterialTheme.typography.labelMedium,
            color = TextDim,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun TopDestinationsCard(stats: PacketStats) {
    val maxCount = stats.topDestinations.firstOrNull()?.second?.toFloat() ?: 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "TOP DESTINATIONS",
            style = MaterialTheme.typography.labelMedium,
            color = TextDim,
            fontWeight = FontWeight.Bold
        )
        stats.topDestinations.forEach { (ip, count) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    ip,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(120.dp),
                    maxLines = 1
                )
                LinearProgressIndicator(
                    progress = { (count / maxCount).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CyberBlue,
                    trackColor = SurfaceVariantDark
                )
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberBlue,
                    modifier = Modifier.width(32.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PacketRateChart(packets: List<CapturedPacket>) {
    val bucketMs = 2000L
    val numBuckets = 15
    val windowMs = bucketMs * numBuckets  // 30s

    // Tick every second to keep chart live
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }

    // Derive buckets from recentPackets — no ViewModel change needed
    val buckets = remember(packets, nowMs) {
        val cutoff = nowMs - windowMs
        val inWindow = packets.filter { it.timestamp >= cutoff }
        Array(numBuckets) { i ->
            val start = nowMs - (numBuckets - i) * bucketMs
            val end   = start + bucketMs
            inWindow.filter { it.timestamp in start until end }
        }
    }
    val maxCount = buckets.maxOf { it.size }.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Timeline, null, tint = CyberGreen, modifier = Modifier.size(14.dp))
            Text(
                "PACKET TIMELINE  (2s buckets · 30s window)",
                style = MaterialTheme.typography.labelMedium,
                color = TextDim,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "max $maxCount/2s",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
        Spacer(Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val barW = size.width / numBuckets
            val chartH = size.height - 16.dp.toPx()

            buckets.forEachIndexed { i, bucket ->
                val barH = (bucket.size.toFloat() / maxCount) * chartH
                val x = i * barW
                val y = chartH - barH

                val tcp  = bucket.count { it.protocol == IpProtocol.TCP }
                val udp  = bucket.count { it.protocol == IpProtocol.UDP }
                val icmp = bucket.count { it.protocol == IpProtocol.ICMP }
                val barColor = when {
                    bucket.isEmpty()                    -> Color.Transparent
                    tcp  >= udp  && tcp  >= icmp        -> CyberBlue
                    udp  >= icmp                        -> CyberGreen
                    else                                -> CyberYellow
                }
                val isCurrent = (i == numBuckets - 1)

                // Active bucket highlight
                if (isCurrent) {
                    drawRect(
                        color = CyberGreen.copy(alpha = 0.06f),
                        topLeft = Offset(x, 0f),
                        size = Size(barW, chartH)
                    )
                }

                if (barH > 0f) {
                    drawRect(
                        color = barColor.copy(alpha = if (isCurrent) 0.9f else 0.55f),
                        topLeft = Offset(x + 1.5f, y),
                        size = Size(barW - 3f, barH)
                    )
                    // Lighter top edge
                    drawRect(
                        color = barColor.copy(alpha = 0.4f),
                        topLeft = Offset(x + 1.5f, y),
                        size = Size(barW - 3f, 2.dp.toPx())
                    )
                }
            }

            // Baseline
            drawLine(
                color = CardBorderDark,
                start = Offset(0f, chartH),
                end   = Offset(size.width, chartH),
                strokeWidth = 1f
            )
        }

        // Time axis labels
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "-30s",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                modifier = Modifier.weight(1f)
            )
            Text(
                "-20s",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
            Text(
                "-10s",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
            Text(
                "now",
                style = MaterialTheme.typography.labelSmall,
                color = CyberGreen,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.End)
            )
        }
    }
}

@Composable
private fun DnsQueriesCard(queries: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Dns, null, tint = CyberYellow, modifier = Modifier.size(14.dp))
            Text(
                "DNS QUERIES  (${queries.size})",
                style = MaterialTheme.typography.labelMedium,
                color = TextDim,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(2.dp))
        queries.forEach { domain ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(CyberYellow, RoundedCornerShape(50))
                )
                Text(
                    domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
