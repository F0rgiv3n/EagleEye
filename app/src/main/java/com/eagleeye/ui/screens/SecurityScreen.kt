package com.eagleeye.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleeye.data.SecurityScore
import com.eagleeye.data.Threat
import com.eagleeye.data.ThreatLevel
import com.eagleeye.modules.security.AuditState
import com.eagleeye.modules.security.SecurityViewModel
import com.eagleeye.ui.theme.*

@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel,
    toolsViewModel: com.eagleeye.modules.tools.ToolsViewModel? = null,
    wifiViewModel: com.eagleeye.modules.wifi.WifiViewModel? = null,
    lanViewModel: com.eagleeye.modules.lan.LanViewModel? = null
) {
    val state by viewModel.auditState.collectAsState()
    val exportIntent by toolsViewModel?.exportIntent?.collectAsState() ?: remember { mutableStateOf(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Launch share intent when export is ready
    LaunchedEffect(exportIntent) {
        exportIntent?.let {
            context.startActivity(android.content.Intent.createChooser(it, "Share Report"))
            toolsViewModel?.clearExportIntent()
        }
    }

    val wifiInfo by wifiViewModel?.connectionInfo?.collectAsState() ?: remember { mutableStateOf(com.eagleeye.data.WifiConnectionInfo()) }
    val lanDevices by lanViewModel?.savedDevices?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

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
                Text("SECURITY AUDIT", style = MaterialTheme.typography.headlineMedium, color = CyberGreen)
                Text(
                    text = when (state) {
                        is AuditState.Idle -> "Tap to run audit"
                        is AuditState.Running -> "Scanning for threats..."
                        is AuditState.Result -> "Last scan: just now"
                        is AuditState.Error -> "Scan failed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Export button (only when we have results)
            if (toolsViewModel != null && state is AuditState.Result) {
                var showExportMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showExportMenu = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberBlue.copy(alpha = 0.12f))
                            .border(1.dp, CyberBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Share, null, tint = CyberBlue)
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false },
                        modifier = Modifier.background(SurfaceVariantDark)
                    ) {
                        val score = (state as? AuditState.Result)?.score
                        DropdownMenuItem(
                            text = { Text("Export JSON", style = MaterialTheme.typography.bodySmall, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Code, null, tint = CyberGreen, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showExportMenu = false
                                toolsViewModel.exportReport(wifiInfo, score, lanDevices, asJson = true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Text", style = MaterialTheme.typography.bodySmall, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Article, null, tint = CyberBlue, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showExportMenu = false
                                toolsViewModel.exportReport(wifiInfo, score, lanDevices, asJson = false)
                            }
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.runAudit() },
                enabled = state !is AuditState.Running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen.copy(alpha = 0.15f),
                    contentColor = CyberGreen,
                    disabledContainerColor = SurfaceVariantDark,
                    disabledContentColor = TextDim
                ),
                border = BorderStroke(
                    1.dp,
                    if (state !is AuditState.Running) CyberGreen.copy(0.5f) else TextDim.copy(0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state is AuditState.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = CyberGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("RUNNING")
                } else {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SCAN")
                }
            }
            } // close Row for buttons
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val s = state) {
            is AuditState.Running -> ScanningState()
            is AuditState.Result -> SecurityContent(s.score)
            is AuditState.Error -> ErrorState(s.message) { viewModel.runAudit() }
            is AuditState.Idle -> IdleState { viewModel.runAudit() }
        }
    }
}

@Composable
private fun SecurityContent(score: SecurityScore) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SecurityScoreCard(score) }
        item { ScoreBreakdown(score) }

        if (score.threats.isNotEmpty()) {
            item {
                Text(
                    text = "THREATS (${score.threats.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberGreen,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(score.threats, key = { it.id }) { threat ->
                ThreatCard(threat)
            }
        } else {
            item { AllClearCard() }
        }
    }
}

@Composable
private fun SecurityScoreCard(score: SecurityScore) {
    val scoreColor = when (score.gradeColor) {
        "GREEN" -> CyberGreen
        "YELLOW" -> CyberYellow
        "ORANGE" -> CyberOrange
        else -> CyberRed
    }

    val animatedScore by animateIntAsState(
        targetValue = score.total,
        animationSpec = tween(1200, easing = EaseOut),
        label = "score"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, scoreColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECURITY SCORE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$animatedScore",
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp),
                        color = scoreColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / 100",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val criticalCount = score.threats.count { it.level == ThreatLevel.CRITICAL }
                val highCount = score.threats.count { it.level == ThreatLevel.HIGH }
                Text(
                    text = buildString {
                        if (criticalCount > 0) append("$criticalCount CRITICAL  ")
                        if (highCount > 0) append("$highCount HIGH  ")
                        if (criticalCount == 0 && highCount == 0) append("No critical threats")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (criticalCount > 0) CyberRed else if (highCount > 0) CyberOrange else CyberGreen
                )
            }

            // Grade circle
            Box(
                modifier = Modifier.size(88.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweep = (score.total / 100f) * 360f
                    drawArc(
                        color = scoreColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = score.grade,
                    style = MaterialTheme.typography.headlineMedium,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScoreBreakdown(score: SecurityScore) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("SCORE BREAKDOWN", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
        HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

        ScoreRow("Encryption",     score.encryption,      30)
        ScoreRow("No Evil Twin",   score.noEvilTwin,      15)
        ScoreRow("ARP Integrity",  score.noOpenPorts,     15)
        ScoreRow("Unknown Devices",score.noUnknownDevices,15)
        ScoreRow("No WPS",         score.noWps,           15)
        ScoreRow("DNS Integrity",  score.dnsIntegrity,    10)
    }
}

@Composable
private fun ScoreRow(label: String, score: Int, max: Int) {
    val pct = score.toFloat() / max.toFloat()
    val color = when {
        pct >= 1f -> CyberGreen
        pct >= 0.5f -> CyberYellow
        else -> CyberRed
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(130.dp)
        )
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = SurfaceVariantDark
        )
        Text(
            text = "$score/$max",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun ThreatCard(threat: Threat) {
    var expanded by remember { mutableStateOf(threat.level == ThreatLevel.CRITICAL) }

    val (borderColor, bgColor, icon) = when (threat.level) {
        ThreatLevel.CRITICAL -> Triple(CyberRed, CyberRed.copy(alpha = 0.05f), Icons.Default.GppBad)
        ThreatLevel.HIGH     -> Triple(CyberOrange, CyberOrange.copy(alpha = 0.05f), Icons.Default.Warning)
        ThreatLevel.MEDIUM   -> Triple(CyberYellow, CyberYellow.copy(alpha = 0.05f), Icons.Default.ReportProblem)
        ThreatLevel.LOW      -> Triple(CyberBlue, CyberBlue.copy(alpha = 0.05f), Icons.Default.Info)
        ThreatLevel.INFO     -> Triple(TextDim, Color.Transparent, Icons.Default.Info)
    }

    val levelLabel = threat.level.name

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = borderColor, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = threat.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = levelLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = borderColor
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDark)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(color = borderColor.copy(alpha = 0.2f))
                Text(threat.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, null, tint = CyberYellow, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Text(threat.recommendation, style = MaterialTheme.typography.bodySmall, color = CyberYellow)
                }
            }
        }
    }
}

@Composable
private fun AllClearCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberGreen.copy(alpha = 0.05f))
            .border(1.dp, CyberGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.VerifiedUser, null, tint = CyberGreen, modifier = Modifier.size(40.dp))
            Text("No Threats Detected", style = MaterialTheme.typography.titleMedium, color = CyberGreen)
            Text("Network appears secure", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
    }
}

@Composable
private fun ScanningState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
                label = "rot"
            )
            Icon(
                Icons.Default.Shield, null,
                tint = CyberGreen,
                modifier = Modifier.size(56.dp).rotate(rotation)
            )
            Text("Running security audit...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("Checking for ARP spoof, evil twin, DNS hijack...", style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
    }
}

@Composable
private fun IdleState(onScan: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Shield, null, tint = TextDim, modifier = Modifier.size(56.dp))
            Text("Security Audit", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Text("Tap SCAN to analyse your network", style = MaterialTheme.typography.bodySmall, color = TextDim)
            Button(onClick = onScan, colors = ButtonDefaults.buttonColors(containerColor = CyberGreen)) {
                Text("Start Audit", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorState(msg: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, null, tint = CyberRed, modifier = Modifier.size(48.dp))
            Text("Audit failed", style = MaterialTheme.typography.titleMedium, color = CyberRed)
            Text(msg, style = MaterialTheme.typography.bodySmall, color = TextDim)
            OutlinedButton(onClick = onRetry, border = BorderStroke(1.dp, CyberRed)) {
                Text("Retry", color = CyberRed)
            }
        }
    }
}
