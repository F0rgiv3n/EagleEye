package com.eagleeye.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.eagleeye.MainActivity

class SecurityWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("eagle_widget_prefs", Context.MODE_PRIVATE)
        val grade = prefs.getString("security_grade", "?") ?: "?"
        val threatCount = prefs.getInt("threat_count", 0)
        val lastScan = prefs.getString("last_scan", "Not scanned") ?: "Not scanned"

        val gradeColor = when (grade) {
            "A" -> Color(0xFF00FF88)
            "B" -> Color(0xFF00D4FF)
            "C" -> Color(0xFFFFD60A)
            "D" -> Color(0xFFFF9500)
            "F" -> Color(0xFFFF3B5C)
            else -> Color(0xFF8BA3C1)
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0E1A))
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.Top,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text(
                        text = "EAGLEEYE",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF00FF88)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )

                    Spacer(GlanceModifier.height(4.dp))

                    Text(
                        text = grade,
                        style = TextStyle(
                            color = ColorProvider(gradeColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp
                        )
                    )

                    Spacer(GlanceModifier.height(4.dp))

                    Text(
                        text = "THREATS: $threatCount",
                        style = TextStyle(
                            color = ColorProvider(
                                if (threatCount > 0) Color(0xFFFF3B5C) else Color(0xFF00FF88)
                            ),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(GlanceModifier.height(2.dp))

                    Text(
                        text = "Last scan: $lastScan",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF4A6080)),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}
