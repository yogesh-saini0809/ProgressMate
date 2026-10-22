package com.progressmate.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CircularProgressBadge(
    percentage: Float,
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(targetValue = percentage, label = "progress")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(140.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 14.dp.toPx()
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = Color(0xFF10B981),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$completedCount of $totalCount done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
