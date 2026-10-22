package com.progressmate.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen(
    weeklyFocusHours: List<Float>,
    totalFocusMinutes: Long,
    totalCompletedTasks: Int,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Your Growth Summary", style = MaterialTheme.typography.headlineMedium)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricColumn("Consistency", "$streakDays Days")
                MetricColumn("Total Focus", "${totalFocusMinutes / 60}h ${totalFocusMinutes % 60}m")
                MetricColumn("Completed", "$totalCompletedTasks")
            }
        }

        Text("Last 7 Days (Hours Focused)", style = MaterialTheme.typography.titleMedium)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxVal = (weeklyFocusHours.maxOrNull() ?: 1f).coerceAtLeast(1f)
                    val barWidth = size.width / (weeklyFocusHours.size * 2)
                    val spacing = barWidth

                    weeklyFocusHours.forEachIndexed { index, value ->
                        val barHeight = (value / maxVal) * size.height
                        val left = index * (barWidth + spacing) + spacing / 2
                        val top = size.height - barHeight

                        drawRoundRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Honest metrics: Tasks are only marked when you verify them. Consistency comes from daily micro-steps.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}
