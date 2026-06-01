package com.eagleeye

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eagleeye.modules.wifi.WifiViewModel
import com.eagleeye.modules.lan.LanViewModel
import com.eagleeye.modules.security.AuditState
import com.eagleeye.modules.security.SecurityViewModel
import com.eagleeye.modules.tools.ToolsViewModel
import com.eagleeye.modules.mac.MacViewModel
import com.eagleeye.modules.monitor.MonitorViewModel
import com.eagleeye.modules.iot.IoTViewModel
import com.eagleeye.modules.settings.SettingsViewModel
import com.eagleeye.modules.packet.PacketViewModel
import com.eagleeye.modules.bluetooth.BluetoothViewModel
import com.eagleeye.ui.screens.*
import com.eagleeye.ui.theme.*

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled gracefully in ViewModels */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }

        setContent {
            EagleEyeTheme {
                EagleEyeApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object NetworkScan : Screen("networks", "Networks", Icons.Default.Wifi)
    object LanScanner : Screen("lan", "LAN", Icons.Default.DeviceHub)
    object Security : Screen("security", "Security", Icons.Default.Shield)
    object Tools : Screen("tools", "Tools", Icons.Default.Build)
    object Mac : Screen("mac", "MAC", Icons.Default.PrivacyTip)
    object Monitor : Screen("monitor", "Monitor", Icons.Default.Radar)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.NetworkScan,
    Screen.LanScanner,
    Screen.Security,
    Screen.Tools,
    Screen.Mac,
    Screen.Monitor,
    Screen.Settings
)

@Composable
fun EagleEyeApp() {
    val wifiViewModel: WifiViewModel = viewModel()
    val lanViewModel: LanViewModel = viewModel()
    val securityViewModel: SecurityViewModel = viewModel()
    val toolsViewModel: ToolsViewModel = viewModel()
    val macViewModel: MacViewModel = viewModel()
    val monitorViewModel: MonitorViewModel = viewModel()
    val iotViewModel: IoTViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val packetViewModel: PacketViewModel = viewModel()
    val btViewModel: BluetoothViewModel = viewModel()
    val screenSaver = remember {
        Saver<Screen, String>(
            save = { it.route },
            restore = { route -> bottomNavItems.firstOrNull { s -> s.route == route } ?: Screen.Dashboard }
        )
    }
    var currentScreen: Screen by rememberSaveable(stateSaver = screenSaver) {
        mutableStateOf(Screen.Dashboard)
    }
    val unreadCount by monitorViewModel.unreadCount.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val wifiInfo by wifiViewModel.connectionInfo.collectAsState()
    val auditState by securityViewModel.auditState.collectAsState()
    val savedDevices by lanViewModel.savedDevices.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BackgroundDark,
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                currentScreen = screen
                                if (screen == Screen.Monitor) monitorViewModel.markAllRead()
                            },
                            icon = {
                                BadgedBox(badge = {
                                    if (screen == Screen.Monitor && unreadCount > 0) {
                                        Badge(containerColor = CyberRed) {
                                            Text("$unreadCount", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }) {
                                    Icon(screen.icon, contentDescription = screen.label)
                                }
                            },
                            label = {
                                Text(screen.label, style = MaterialTheme.typography.labelMedium)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberGreen,
                                selectedTextColor = CyberGreen,
                                unselectedIconColor = TextDim,
                                unselectedTextColor = TextDim,
                                indicatorColor = CyberGreen.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(120)))
                    },
                    label = "screen-transition"
                ) { screen ->
                    when (screen) {
                        Screen.Dashboard -> DashboardScreen(wifiViewModel, toolsViewModel)
                        Screen.NetworkScan -> NetworkScanScreen(wifiViewModel)
                        Screen.LanScanner -> LanScannerScreen(lanViewModel, iotViewModel, wifiViewModel)
                        Screen.Security -> SecurityScreen(securityViewModel, toolsViewModel, wifiViewModel, lanViewModel)
                        Screen.Tools -> ToolsScreen(
                            viewModel = toolsViewModel,
                            packetViewModel = packetViewModel,
                            btViewModel = btViewModel,
                            wifiInfo = wifiInfo,
                            securityScore = (auditState as? AuditState.Result)?.score,
                            lanDevices = savedDevices
                        )
                        Screen.Mac -> MacScreen(macViewModel)
                        Screen.Monitor -> MonitorScreen(monitorViewModel)
                        Screen.Settings -> SettingsScreen(settingsViewModel)
                    }
                }
            }
        }

        if (!settings.onboardingDone) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
            ) {
                OnboardingScreen(onDone = { settingsViewModel.setOnboardingDone() })
            }
        }
    }
}
