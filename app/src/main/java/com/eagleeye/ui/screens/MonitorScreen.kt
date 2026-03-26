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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eagleeye.data.*
import com.eagleeye.modules.monitor.MonitorViewModel
import com.eagleeye.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonitorScreen(viewModel: MonitorViewModel) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsState()
    val unread by viewModel.unreadCount.collectAsState()
    val config by viewModel.config.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    // Tab state
    var selectedTab by remember { mutableStateOf(0) }

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
                Text("MONITOR", style = MaterialTheme.typography.headlineMedium, color = CyberGreen)
                Text(
                    if (isRunning) "Active — scanning every ${config.intervalMinutes} min"
                    else "Inactive — tap to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRunning) CyberGreen else TextDim
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Settings
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                ) {
                    Icon(Icons.Default.Settings, null, tint = TextSecondary)
                }
                // Start/Stop
                MonitorToggleButton(isRunning) {
                    if (isRunning) viewModel.stopMonitor(context)
                    else viewModel.startMonitor(context)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Stats row
        val criticalCount = events.count { it.severity == EventSeverity.CRITICAL }
        val highCount     = events.count { it.severity == EventSeverity.HIGH }
        val totalToday    = events.count {
            it.timestamp > System.currentTimeMillis() - 86400_000L &&
            it.type != EventType.SCAN_COMPLETE
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventStatChip("CRITICAL", "$criticalCount", CyberRed,    Modifier.weight(1f))
            EventStatChip("HIGH",     "$highCount",     CyberOrange,  Modifier.weight(1f))
            EventStatChip("TODAY",    "$totalToday",    CyberBlue,    Modifier.weight(1f))
            EventStatChip("UNREAD",   "$unread",        if (unread > 0) CyberYellow else TextDim, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Tabs: Timeline / Threats only
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = CyberGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberGreen
                )
            }
        ) {
            listOf("TIMELINE", "THREATS ONLY").forEachIndexed { i, label ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTab == i) CyberGreen else TextDim
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (unread > 0) {
                TextButton(onClick = { viewModel.markAllRead() }) {
                    Icon(Icons.Default.DoneAll, null, Modifier.size(14.dp), tint = CyberBlue)
                    Spacer(Modifier.width(4.dp))
                    Text("Mark all read", style = MaterialTheme.typography.bodySmall, color = CyberBlue)
                }
            }
            Spacer(Modifier.weight(1f))
            if (events.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAll() }) {
                    Icon(Icons.Default.DeleteSweep, null, Modifier.size(14.dp), tint = TextDim)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
            }
        }

        val filteredEvents = when (selectedTab) {
            1 -> events.filter { it.type != EventType.SCAN_COMPLETE && it.type != EventType.MONITOR_STARTED && it.type != EventType.MONITOR_STOPPED }
            else -> events
        }

        if (filteredEvents.isEmpty()) {
            EmptyTimeline(isRunning)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredEvents, key = { it.id }) { event ->
                    EventCard(event)
                }
            }
        }
    }

    // Settings sheet
    if (showSettings) {
        MonitorSettingsSheet(
            config = config,
            onDismiss = { showSettings = false },
            onSave = { newConfig ->
                viewModel.updateConfig(newConfig)
                showSettings = false
                if (isRunning) {
                    viewModel.stopMonitor(context)
                    viewModel.startMonitor(context)
                }
            }
        )
    }
}

// ── Event Card ────────────────────────────────────────────────────────────────

@Composable
private fun EventCard(event: NetworkEvent) {
    val (color, icon) = eventStyle(event)
    val isImportant = event.severity in listOf(EventSeverity.CRITICAL, EventSeverity.HIGH)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isImportant) color.copy(alpha = 0.05f) else SurfaceDark
            )
            .border(
                1.dp,
                if (isImportant) color.copy(alpha = 0.35f) else CardBorderDark,
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon + timeline line
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            if (!event.isRead && isImportant) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isImportant) TextPrimary else TextSecondary,
                    fontWeight = if (isImportant) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatEventTime(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(event.detail, style = MaterialTheme.typography.bodySmall, color = TextDim)
            if (event.ssid.isNotBlank() || event.ip.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (event.ssid.isNotBlank()) {
                        MiniTag(event.ssid, CyberBlue)
                    }
                    if (event.ip.isNotBlank()) {
                        MiniTag(event.ip, TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTag(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

private fun eventStyle(event: NetworkEvent): Pair<Color, ImageVector> = when (event.type) {
    EventType.NEW_DEVICE      -> CyberOrange to Icons.Default.DeviceUnknown
    EventType.DEVICE_GONE     -> TextDim to Icons.Default.DeviceHub
    EventType.ARP_SPOOF       -> CyberRed to Icons.Default.GppBad
    EventType.EVIL_TWIN       -> CyberRed to Icons.Default.WifiFind
    EventType.DNS_CHANGED     -> CyberOrange to Icons.Default.Dns
    EventType.WPS_DETECTED    -> CyberYellow to Icons.Default.Warning
    EventType.OPEN_NETWORK    -> CyberYellow to Icons.Default.LockOpen
    EventType.SCAN_COMPLETE   -> TextDim to Icons.Default.Check
    EventType.MONITOR_STARTED -> CyberGreen to Icons.Default.PlayArrow
    EventType.MONITOR_STOPPED -> TextDim to Icons.Default.Stop
}

// ── Monitor Toggle Button ─────────────────────────────────────────────────────

@Composable
private fun MonitorToggleButton(isRunning: Boolean, onClick: () -> Unit) {
    val color = if (isRunning) CyberRed else CyberGreen
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            null, modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (isRunning) "STOP" else "START",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Settings Sheet ────────────────────────────────────────────────────────────

@Composable
private fun MonitorSettingsSheet(
    config: MonitorConfig,
    onDismiss: () -> Unit,
    onSave: (MonitorConfig) -> Unit
) {
    var intervalIdx by remember {
        mutableStateOf(listOf(5, 10, 15, 30, 60).indexOfFirst { it == config.intervalMinutes }.coerceAtLeast(0))
    }
    var notifyNew     by remember { mutableStateOf(config.notifyNewDevice) }
    var notifyArp     by remember { mutableStateOf(config.notifyArpSpoof) }
    var notifyTwin    by remember { mutableStateOf(config.notifyEvilTwin) }
    var notifyDns     by remember { mutableStateOf(config.notifyDnsChange) }
    var notifyWeak    by remember { mutableStateOf(config.notifyWeakSecurity) }

    val intervals = listOf(5, 10, 15, 30, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text("Monitor Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Interval
                Text("Scan Interval", style = MaterialTheme.typography.labelMedium, color = TextDim)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    intervals.forEachIndexed { i, min ->
                        val selected = intervalIdx == i
                        Text(
                            "${min}m",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) CyberGreen else TextDim,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) CyberGreen.copy(alpha = 0.15f) else SurfaceVariantDark)
                                .border(1.dp, if (selected) CyberGreen.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { intervalIdx = i }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = CardBorderDark)
                Text("Notifications", style = MaterialTheme.typography.labelMedium, color = TextDim)

                SettingToggle("New devices on LAN", notifyNew, CyberOrange) { notifyNew = it }
                SettingToggle("ARP spoofing / MITM", notifyArp, CyberRed) { notifyArp = it }
                SettingToggle("Evil twin AP", notifyTwin, CyberRed) { notifyTwin = it }
                SettingToggle("DNS server change", notifyDns, CyberOrange) { notifyDns = it }
                SettingToggle("Weak security (WEP/WPS)", notifyWeak, CyberYellow) { notifyWeak = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(config.copy(
                    intervalMinutes = intervals[intervalIdx],
                    notifyNewDevice = notifyNew,
                    notifyArpSpoof = notifyArp,
                    notifyEvilTwin = notifyTwin,
                    notifyDnsChange = notifyDns,
                    notifyWeakSecurity = notifyWeak
                ))
            }) {
                Text("Save", color = CyberGreen, fontWeight = FontWeight.Bold)
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
private fun SettingToggle(label: String, checked: Boolean, activeColor: Color, onCheck: (Boolean) -> Unit) {
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
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (checked) activeColor else TextDim)
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheck,
            colors = SwitchDefaults.colors(
                checkedThumbColor = activeColor,
                checkedTrackColor = activeColor.copy(alpha = 0.3f),
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = SurfaceVariantDark
            )
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun EventStatChip(label: String, value: String, color: Color, modifier: Modifier) {
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
private fun EmptyTimeline(isRunning: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.History, null, tint = TextDim, modifier = Modifier.size(56.dp))
            Text("No events yet", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Text(
                if (isRunning) "Waiting for next scan cycle..."
                else "Start the monitor to begin tracking events",
                style = MaterialTheme.typography.bodySmall, color = TextDim
            )
        }
    }
}

private fun formatEventTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000L     -> "just now"
        diff < 3_600_000L  -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ts))
    }
}

// Extension for tab indicator
private fun Modifier.tabIndicatorOffset(tabPosition: androidx.compose.material3.TabPosition) =
    this.then(
        Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = tabPosition.left)
            .width(tabPosition.width)
    )
