package com.eagleeye

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eagleeye.modules.wifi.WifiViewModel
import com.eagleeye.modules.lan.LanViewModel
import com.eagleeye.ui.screens.*
import com.eagleeye.ui.theme.*

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled gracefully in ViewModels */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
            )
        )

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
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.NetworkScan,
    Screen.LanScanner,
    Screen.Security,
    Screen.Tools
)

@Composable
fun EagleEyeApp() {
    val wifiViewModel: WifiViewModel = viewModel()
    val lanViewModel: LanViewModel = viewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

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
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(screen.icon, contentDescription = screen.label)
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
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(wifiViewModel)
                Screen.NetworkScan -> NetworkScanScreen(wifiViewModel)
                Screen.LanScanner -> LanScannerScreen(lanViewModel)
                Screen.Security -> PlaceholderScreen(
                    icon = Icons.Default.Shield,
                    title = "Security Engine",
                    subtitle = "Coming in Part 3"
                )
                Screen.Tools -> PlaceholderScreen(
                    icon = Icons.Default.Build,
                    title = "Network Tools",
                    subtitle = "Coming in Part 4"
                )
            }
        }
    }
}
