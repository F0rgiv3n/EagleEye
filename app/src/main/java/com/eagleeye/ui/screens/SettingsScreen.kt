package com.eagleeye.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

        // API Keys
        SettingsSection(title = "API KEYS") {
            Text(
                "Optional API keys to unlock full threat intelligence features.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
            Spacer(Modifier.height(4.dp))
            ApiKeyField(
                icon = Icons.Default.Radar,
                label = "Shodan API Key",
                value = settings.shodanApiKey,
                onSave = { viewModel.setApiKey("shodan", it) }
            )
            Spacer(Modifier.height(8.dp))
            ApiKeyField(
                icon = Icons.Default.GppBad,
                label = "AbuseIPDB API Key",
                value = settings.abuseIpDbKey,
                onSave = { viewModel.setApiKey("abusipdb", it) }
            )
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

            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

            SettingsSwitchRow(
                icon = Icons.Default.WifiFind,
                label = "Auto-scan on Connect",
                subtitle = "Run LAN scan when Wi-Fi connection is established",
                checked = settings.autoScanOnConnect,
                activeColor = CyberBlue
            ) { viewModel.setAutoScanOnConnect(it) }
        }

        // Demo mode — for portfolio screenshots
        SettingsSection(title = "DEMO MODE") {
            SettingsSwitchRow(
                icon = Icons.Default.Visibility,
                label = "Show demo data",
                subtitle = "Fill LAN, Security, and Monitor with synthetic data so portfolio screenshots don't leak real network state. No live scans run.",
                checked = settings.demoMode,
                activeColor = CyberOrange
            ) { viewModel.setDemoMode(it) }
        }

        // Trusted Networks
        SettingsSection(title = "TRUSTED NETWORKS") {
            Text(
                "SSIDs marked as trusted are treated as safe networks. Unknown networks will trigger a warning.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
            Spacer(Modifier.height(8.dp))

            if (settings.trustedSsids.isEmpty()) {
                Text(
                    "No trusted networks configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    settings.trustedSsids.sorted().forEach { ssid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberGreen.copy(alpha = 0.06f))
                                .border(1.dp, CyberGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.WifiProtectedSetup, null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(ssid, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            }
                            IconButton(
                                onClick = { viewModel.removeTrustedSsid(ssid) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close, null,
                                    tint = TextDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            AddSsidField { viewModel.addTrustedSsid(it) }
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
private fun ApiKeyField(
    icon: ImageVector,
    label: String,
    value: String,
    onSave: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }
    val isDirty = text != value

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = TextDim, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { visible = !visible }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = TextDim, modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isDirty) {
                        IconButton(onClick = { onSave(text); }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Save, null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberGreen.copy(alpha = 0.5f),
                unfocusedBorderColor = CardBorderDark,
                focusedLabelColor = CyberGreen,
                unfocusedLabelColor = TextDim,
                cursorColor = CyberGreen,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    if (value.isNotBlank()) "Key saved" else "Enter API key",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
        )
        if (isDirty) {
            Text(
                "Tap save icon to apply",
                style = MaterialTheme.typography.labelSmall,
                color = CyberGreen.copy(alpha = 0.7f)
            )
        } else if (value.isNotBlank()) {
            Text(
                "Key saved (${value.length} chars)",
                style = MaterialTheme.typography.labelSmall,
                color = CyberGreen.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AddSsidField(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Add SSID (e.g. HomeNetwork)", style = MaterialTheme.typography.bodySmall, color = TextDim) },
        trailingIcon = {
            IconButton(
                onClick = { if (text.isNotBlank()) { onAdd(text); text = "" } },
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.Default.Add, null, tint = if (text.isNotBlank()) CyberGreen else TextDim, modifier = Modifier.size(18.dp))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberGreen.copy(alpha = 0.5f),
            unfocusedBorderColor = CardBorderDark,
            cursorColor = CyberGreen,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(8.dp)
    )
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
