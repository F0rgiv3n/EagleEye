package com.eagleeye.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eagleeye.ui.theme.*

@Composable
fun PlaceholderScreen(icon: ImageVector, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberGreen.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextSecondary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
    }
}
