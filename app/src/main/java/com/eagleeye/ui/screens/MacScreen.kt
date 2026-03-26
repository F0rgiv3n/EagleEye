package com.eagleeye.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.eagleeye.data.MacInfo
import com.eagleeye.data.MacProfile
import com.eagleeye.data.MacType
import com.eagleeye.modules.mac.MacViewModel
import com.eagleeye.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MacScreen(viewModel: MacViewModel) {
    val macInfo by viewModel.macInfo.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val result by viewModel.actionResult.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val newMacPreview by viewModel.newMacPreview.collectAsState()

    // Load MAC info on first open
    LaunchedEffect(Unit) {
        if (macInfo == null) viewModel.refreshMacInfo()
    }

    // Show snackbar for results
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(result) {
        result?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResult()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceVariantDark,
                    contentColor = CyberGreen,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MAC PRIVACY", style = MaterialTheme.typography.headlineMedium, color = CyberGreen)
                    Text("Address spoofing & per-network profiles", style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
                IconButton(onClick = { viewModel.refreshMacInfo() }, enabled = !loading) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberGreen, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, null, tint = CyberGreen)
                    }
                }
            }

            // Current MAC card
            macInfo?.let { MacInfoCard(it) }

            // Change MAC section
            ChangeMacCard(
                newMacPreview = newMacPreview,
                onPreviewChange = viewModel::setNewMacPreview,
                onGenerate = viewModel::generateRandomMac,
                onApply = { viewModel.applyMac(newMacPreview) },
                onRandomize = viewModel::resetToRandom,
                loading = loading
            )

            // Per-network profiles
            ProfilesCard(
                profiles = profiles,
                currentSsid = viewModel.currentSsid,
                onSave = { ssid, mac, rotate, hours ->
                    viewModel.saveProfile(ssid, mac, rotate, hours)
                },
                onDelete = viewModel::deleteProfile,
                onApplyForCurrent = viewModel::applyProfileForCurrentNetwork
            )
        }
    }
}

// ── Current MAC Card ──────────────────────────────────────────────────────────

@Composable
private fun MacInfoCard(info: MacInfo) {
    val typeColor = when (info.type) {
        MacType.RANDOMIZED -> CyberGreen
        MacType.REAL -> CyberOrange
        MacType.CUSTOM -> CyberBlue
        MacType.UNKNOWN -> TextDim
    }
    val typeLabel = when (info.type) {
        MacType.RANDOMIZED -> "RANDOMIZED"
        MacType.REAL -> "REAL HARDWARE"
        MacType.CUSTOM -> "CUSTOM"
        MacType.UNKNOWN -> "UNKNOWN"
    }
    val typeIcon = when (info.type) {
        MacType.RANDOMIZED -> Icons.Default.VerifiedUser
        MacType.REAL -> Icons.Default.Warning
        MacType.CUSTOM -> Icons.Default.Edit
        MacType.UNKNOWN -> Icons.Default.Help
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, typeColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CURRENT MAC", style = MaterialTheme.typography.labelMedium, color = TextDim)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(typeColor.copy(alpha = 0.12f))
                    .border(1.dp, typeColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(typeIcon, null, tint = typeColor, modifier = Modifier.size(12.dp))
                Text(typeLabel, style = MaterialTheme.typography.labelMedium, color = typeColor)
            }
        }

        Text(
            text = info.currentMac,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        if (info.vendor.isNotBlank() && info.vendor != "Unknown") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Business, null, tint = TextDim, modifier = Modifier.size(14.dp))
                Text(
                    "Vendor: ${info.vendor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (info.type == MacType.RANDOMIZED) TextDim else CyberOrange
                )
            }
        }

        if (info.type == MacType.REAL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberOrange.copy(alpha = 0.08f))
                    .border(1.dp, CyberOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, null, tint = CyberOrange, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                Text(
                    "Your real hardware MAC is exposed. Enable Android per-network randomization in Wi-Fi settings, or use the randomize option below (requires root).",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberOrange
                )
            }
        }

        if (info.hardwareMac.isNotBlank() && info.hardwareMac != info.currentMac) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hardware MAC", style = MaterialTheme.typography.bodySmall, color = TextDim)
                Text(info.hardwareMac, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Change MAC Card ───────────────────────────────────────────────────────────

@Composable
private fun ChangeMacCard(
    newMacPreview: String,
    onPreviewChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onApply: () -> Unit,
    onRandomize: () -> Unit,
    loading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CHANGE MAC ADDRESS", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
        HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

        Text(
            "Requires root. Changes take effect immediately but revert on reboot unless applied via profile.",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )

        // Custom MAC input
        OutlinedTextField(
            value = newMacPreview,
            onValueChange = onPreviewChange,
            label = { Text("New MAC Address", style = MaterialTheme.typography.labelMedium) },
            placeholder = { Text("AA:BB:CC:DD:EE:FF", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberGreen,
                unfocusedBorderColor = CardBorderDark,
                focusedLabelColor = CyberGreen,
                unfocusedLabelColor = TextDim,
                cursorColor = CyberGreen,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextSecondary,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            trailingIcon = {
                IconButton(onClick = onGenerate) {
                    Icon(Icons.Default.Casino, null, tint = CyberBlue)
                }
            },
            supportingText = {
                Text("Tap 🎲 to generate a random MAC", style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRandomize,
                enabled = !loading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberBlue),
                border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Randomize", style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = onApply,
                enabled = !loading && newMacPreview.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen.copy(alpha = 0.15f),
                    contentColor = CyberGreen,
                    disabledContainerColor = SurfaceVariantDark,
                    disabledContentColor = TextDim
                ),
                border = BorderStroke(1.dp, if (!loading && newMacPreview.isNotBlank()) CyberGreen.copy(0.5f) else TextDim.copy(0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Apply", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Profiles Card ─────────────────────────────────────────────────────────────

@Composable
private fun ProfilesCard(
    profiles: List<MacProfile>,
    currentSsid: String,
    onSave: (String, String, Boolean, Int) -> Unit,
    onDelete: (MacProfile) -> Unit,
    onApplyForCurrent: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PER-NETWORK PROFILES", style = MaterialTheme.typography.labelMedium, color = CyberGreen)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentSsid.isNotEmpty() && profiles.any { it.ssid == currentSsid }) {
                    IconButton(onClick = onApplyForCurrent, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, null, tint = CyberBlue, modifier = Modifier.size(18.dp))
                }
            }
        }

        HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp)

        if (profiles.isEmpty()) {
            Text(
                "No profiles yet. Add a profile to assign a specific MAC to a network.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
        } else {
            profiles.forEach { profile ->
                ProfileRow(
                    profile = profile,
                    isCurrent = profile.ssid == currentSsid,
                    onDelete = { onDelete(profile) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(
            defaultSsid = currentSsid,
            onDismiss = { showAddDialog = false },
            onSave = { ssid, mac, rotate, hours ->
                onSave(ssid, mac, rotate, hours)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ProfileRow(profile: MacProfile, isCurrent: Boolean, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) CyberGreen.copy(alpha = 0.05f) else CardDark)
            .border(1.dp, if (isCurrent) CyberGreen.copy(alpha = 0.3f) else CardBorderDark, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Wifi, null, tint = if (isCurrent) CyberGreen else TextDim, modifier = Modifier.size(14.dp))
                Text(
                    profile.ssid,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (isCurrent) {
                    Text("●", color = CyberGreen, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(profile.mac, style = MaterialTheme.typography.bodySmall, color = CyberBlue)
            if (profile.isAutoRotate) {
                Text(
                    "Auto-rotate every ${profile.rotateIntervalHours}h  •  Last: ${formatTime(profile.lastRotated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = CyberRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddProfileDialog(
    defaultSsid: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, Int) -> Unit
) {
    var ssid by remember { mutableStateOf(defaultSsid) }
    var mac by remember { mutableStateOf("") }
    var autoRotate by remember { mutableStateOf(false) }
    var intervalHours by remember { mutableStateOf("24") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text("Add MAC Profile", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberGreen, unfocusedBorderColor = CardBorderDark,
                    focusedLabelColor = CyberGreen, unfocusedLabelColor = TextDim,
                    cursorColor = CyberGreen, focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextSecondary,
                    focusedContainerColor = SurfaceDark, unfocusedContainerColor = SurfaceDark
                )

                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Network SSID", style = MaterialTheme.typography.labelMedium) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it },
                    label = { Text("MAC Address", style = MaterialTheme.typography.labelMedium) },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = autoRotate,
                        onCheckedChange = { autoRotate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberGreen,
                            checkedTrackColor = CyberGreen.copy(alpha = 0.3f)
                        )
                    )
                    Text("Auto-rotate MAC", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                AnimatedVisibility(visible = autoRotate) {
                    OutlinedTextField(
                        value = intervalHours,
                        onValueChange = { intervalHours = it.filter(Char::isDigit).take(4) },
                        label = { Text("Interval (hours)", style = MaterialTheme.typography.labelMedium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (ssid.isNotBlank() && mac.isNotBlank()) {
                        onSave(ssid, mac, autoRotate, intervalHours.toIntOrNull() ?: 24)
                    }
                }
            ) {
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

private fun formatTime(ts: Long): String {
    if (ts == 0L) return "Never"
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ts))
}
