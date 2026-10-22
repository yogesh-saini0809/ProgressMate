package com.progressmate.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progressmate.app.data.SessionType
import com.progressmate.app.service.TimerService

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val timerState by TimerService.timerFlow.collectAsState()

    val formattedTime = remember(timerState.remainingSeconds) {
        val mins = timerState.remainingSeconds / 60
        val secs = timerState.remainingSeconds % 60
        "%02d:%02d".format(mins, secs)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (timerState.sessionType == SessionType.WORK)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = if (timerState.sessionType == SessionType.WORK) "Deep Focus Session" else "Rest & Recharge",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = formattedTime,
            fontSize = 72.sp,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!timerState.isRunning) {
                Button(
                    onClick = { startTimer(context, 25 * 60, SessionType.WORK) },
                    modifier = Modifier.size(width = 150.dp, height = 54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Start Focus (25m)")
                }
                OutlinedButton(
                    onClick = { startTimer(context, 5 * 60, SessionType.SHORT_BREAK) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    Text("5m Rest")
                }
            } else {
                if (timerState.isPaused) {
                    Button(
                        onClick = { sendAction(context, TimerService.ACTION_RESUME) },
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text("▶")
                    }
                } else {
                    OutlinedButton(
                        onClick = { sendAction(context, TimerService.ACTION_PAUSE) },
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text("⏸")
                    }
                }

                Button(
                    onClick = { sendAction(context, TimerService.ACTION_EXTEND_5) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("+5 Min")
                }

                Button(
                    onClick = { sendAction(context, TimerService.ACTION_STOP) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("End")
                }
            }
        }
    }
}

private fun startTimer(context: Context, durationSec: Long, type: SessionType) {
    val intent = Intent(context, TimerService::class.java).apply {
        action = TimerService.ACTION_START
        putExtra(TimerService.EXTRA_DURATION_SEC, durationSec)
        putExtra(TimerService.EXTRA_TYPE, type.name)
    }
    context.startService(intent)
}

private fun sendAction(context: Context, action: String) {
    val intent = Intent(context, TimerService::class.java).apply {
        this.action = action
    }
    context.startService(intent)
}
