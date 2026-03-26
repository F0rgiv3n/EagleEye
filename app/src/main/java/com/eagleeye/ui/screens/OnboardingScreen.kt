package com.eagleeye.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleeye.ui.theme.*
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: androidx.compose.ui.graphics.Color
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.Shield,
        title = "EagleEye",
        subtitle = "Professional Network Security for Android",
        description = "Monitor your Wi-Fi environment, detect threats in real time, and keep your network safe.",
        accentColor = androidx.compose.ui.graphics.Color(0xFF00FF88)
    ),
    OnboardingPage(
        icon = Icons.Default.DeviceHub,
        title = "LAN Scanner",
        subtitle = "See every device on your network",
        description = "Discover all connected devices with IP, MAC, hostname, and vendor info. Track new arrivals and detect unknown hosts.",
        accentColor = androidx.compose.ui.graphics.Color(0xFF00D4FF)
    ),
    OnboardingPage(
        icon = Icons.Default.Shield,
        title = "Threat Detection",
        subtitle = "Real-time security analysis",
        description = "Detect ARP spoofing, evil twin access points, DNS hijacking, captive portals, WEP networks, and deauthentication attacks.",
        accentColor = androidx.compose.ui.graphics.Color(0xFFFF3B5C)
    ),
    OnboardingPage(
        icon = Icons.Default.Lock,
        title = "Privacy Tools",
        subtitle = "Control your digital footprint",
        description = "Randomize or spoof your MAC address, inspect CVE vulnerabilities, verify SSL certificates, and detect VPN leaks.",
        accentColor = androidx.compose.ui.graphics.Color(0xFFFF9500)
    )
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dot indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { i ->
                    val isSelected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(if (isSelected) 10.dp else 6.dp)
                            .background(
                                if (isSelected) CyberGreen
                                else TextDim
                            )
                    )
                }
            }

            // Button
            val isLast = pagerState.currentPage == pages.size - 1
            Button(
                onClick = {
                    if (isLast) {
                        onDone()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen.copy(alpha = 0.15f),
                    contentColor = CyberGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f))
            ) {
                Text(
                    if (isLast) "GET STARTED" else "NEXT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 100.dp, bottom = 180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(page.accentColor.copy(alpha = 0.25f), page.accentColor.copy(alpha = 0.05f))
                    )
                )
                .border(1.dp, page.accentColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = page.accentColor,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            page.title,
            style = MaterialTheme.typography.displaySmall,
            color = page.accentColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            page.subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Text(
            page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
