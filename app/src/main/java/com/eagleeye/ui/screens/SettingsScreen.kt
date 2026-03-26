package com.eagleeye.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eagleeye.modules.settings.SettingsViewModel
import com.eagleeye.ui.theme.*

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, color = CyberGreen)

        // General
        SettingsSection(title = "GENERAL") {
            Text(
                "Scan Interval",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            val intervals = listOf(5, 10, 15, 30, 60)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                intervals.forEach { min ->
                    val selected = settings.scanIntervalMinutes == min
                    Text(
                        "${min}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) CyberGreen else TextDim,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) CyberGreen.copy(alpha = 0.15f) else SurfaceVariantDark)
                            .border(
                                1.dp,
                                if (selected) CyberGreen.copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.setScanInterval(min) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
            Spacer(Modifier.height(4.dp))

            SettingsSwitchRow(
                icon = Icons.Default.Speed,
                label = "Port Scan Mode",
                subtitle = if (settings.portScanQuickMode) "Quick (top 1000 ports)" else "Full (all 65535 ports)",
                checked = settings.portScanQuickMode,
                activeColor = CyberBlue
            ) { viewModel.setPortScanQuick(it) }
        }

        // Notifications
        SettingsSection(title = "NOTIFICATIONS") {
            SettingsSwitchRow(
                icon = Icons.Default.DeviceUnknown,
                label = "New Device",
                subtitle = "Alert when unknown device joins LAN",
                checked = settings.notifyNewDevice,
                activeColor = CyberOrange
            ) { viewModel.setNotify("newDevice", it) }

            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

            SettingsSwitchRow(
                icon = Icons.Default.GppBad,
                label = "ARP Spoof / MITM",
                subtitle = "Detect man-in-the-middle attacks",
                checked = settings.notifyArpSpoof,
                activeColor = CyberRed
            ) { viewModel.setNotify("arp", it) }

            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

            SettingsSwitchRow(
                icon = Icons.Default.WifiFind,
                label = "Evil Twin AP",
                subtitle = "Detect rogue access points",
                checked = settings.notifyEvilTwin,
                activeColor = CyberRed
            ) { viewModel.setNotify("evilTwin", it) }

            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

            SettingsSwitchRow(
                icon = Icons.Default.Dns,
                label = "DNS Change",
                subtitle = "Monitor router DNS for changes",
                checked = settings.notifyDnsChange,
                activeColor = CyberOrange
            ) { viewModel.setNotify("dns", it) }

            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

            SettingsSwitchRow(
                icon = Icons.Default.Warning,
                label = "Weak Security",
                subtitle = "Open networks, WEP, WPS-enabled APs",
                checked = settings.notifyWeakSecurity,
                activeColor = CyberYellow
            ) { viewModel.setNotify("weakSec", it) }
        }

        // Monitor
        SettingsSection(title = "MONITOR") {
            SettingsSwitchRow(
                icon = Icons.Default.Radar,
                label = "Auto-start on Boot",
                subtitle = "Begin monitoring automatically after reboot",
                checked = settings.autoStartMonitor,
                activeColor = CyberGreen
            ) { viewModel.setAutoStartMonitor(it) }
        }

        // About
        SettingsSection(title = "ABOUT") {
            AboutRow(Icons.Default.Shield, "Version", "1.0.0")
            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
            AboutRow(Icons.Default.Code, "Build", "Release — Kotlin 2.0 / Compose")
            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, null, tint = TextDim, modifier = Modifier.size(16.dp))
                Text(
                    "Professional Wi-Fi security analysis, network management, and cybersecurity auditing for Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = CyberGreen)
        HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    activeColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeColor.copy(alpha = if (checked) 0.15f else 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (checked) activeColor else TextDim, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = activeColor,
                checkedTrackColor = activeColor.copy(alpha = 0.3f),
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = SurfaceVariantDark
            )
        )
    }
}

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = TextDim, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
