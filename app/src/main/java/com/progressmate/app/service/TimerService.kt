package com.progressmate.app.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import com.progressmate.app.data.AppDatabase
import com.progressmate.app.data.SessionType
import com.progressmate.app.data.WorkSessionEntity
import com.progressmate.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TimerService : Service() {
    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var database: AppDatabase

    private var targetDurationSeconds: Long = 25 * 60
    private var isRunning: Boolean = false
    private var isPaused: Boolean = false
    private var baseTime: Long = 0L
    private var timeAlreadyElapsedMs: Long = 0L
    private var currentSessionType = SessionType.WORK
    private var sessionStartEpochMs: Long = 0L

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getLongExtra(EXTRA_DURATION_SEC, 25 * 60)
                val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: SessionType.WORK.name
                startTimer(duration, SessionType.valueOf(typeStr))
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
            ACTION_EXTEND_5 -> extendTimer(5 * 60)
        }
        return START_NOT_STICKY
    }

    private fun startTimer(durationSeconds: Long, type: SessionType) {
        targetDurationSeconds = durationSeconds
        currentSessionType = type
        isRunning = true
        isPaused = false
        timeAlreadyElapsedMs = 0L
        baseTime = SystemClock.elapsedRealtime()
        sessionStartEpochMs = System.currentTimeMillis()

        val notification = buildNotification("Running...", "00:00")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        launchTicker()
    }

    private fun pauseTimer() {
        if (isRunning && !isPaused) {
            isPaused = true
            timeAlreadyElapsedMs += SystemClock.elapsedRealtime() - baseTime
            updateNotification("Paused")
            _timerFlow.value = currentTimerState()
        }
    }

    private fun resumeTimer() {
        if (isRunning && isPaused) {
            isPaused = false
            baseTime = SystemClock.elapsedRealtime()
            updateNotification("Focusing...")
            launchTicker()
        }
    }

    private fun extendTimer(seconds: Long) {
        targetDurationSeconds += seconds
        _timerFlow.value = currentTimerState()
        updateNotification("Extended (+5 min)")
    }

    private fun stopTimer() {
        val finalElapsedSeconds = if (isRunning) {
            val ms = if (isPaused) timeAlreadyElapsedMs else timeAlreadyElapsedMs + (SystemClock.elapsedRealtime() - baseTime)
            ms / 1000
        } else 0L

        if (finalElapsedSeconds > 10) {
            serviceScope.launch(Dispatchers.IO) {
                database.progressDao().insertSession(
                    WorkSessionEntity(
                        startTimeEpochMs = sessionStartEpochMs,
                        endTimeEpochMs = System.currentTimeMillis(),
                        durationSeconds = finalElapsedSeconds,
                        sessionType = currentSessionType
                    )
                )
            }
        }

        isRunning = false
        isPaused = false
        timeAlreadyElapsedMs = 0L
        _timerFlow.value = TimerState(0, 0, false, false, currentSessionType)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun launchTicker() {
        serviceScope.launch {
            while (isRunning && !isPaused) {
                val elapsedMs = timeAlreadyElapsedMs + (SystemClock.elapsedRealtime() - baseTime)
                val elapsedSec = elapsedMs / 1000
                val remainingSec = (targetDurationSeconds - elapsedSec).coerceAtLeast(0)

                _timerFlow.value = TimerState(
                    remainingSeconds = remainingSec,
                    elapsedSeconds = elapsedSec,
                    isRunning = true,
                    isPaused = false,
                    sessionType = currentSessionType
                )

                if (remainingSec <= 0) {
                    stopTimer()
                    break
                }
                updateNotification(formatTime(remainingSec))
                delay(1000)
            }
        }
    }

    private fun currentTimerState(): TimerState {
        val elapsedMs = if (isPaused) timeAlreadyElapsedMs else timeAlreadyElapsedMs + (SystemClock.elapsedRealtime() - baseTime)
        val elapsedSec = elapsedMs / 1000
        val remainingSec = (targetDurationSeconds - elapsedSec).coerceAtLeast(0)
        return TimerState(remainingSec, elapsedSec, isRunning, isPaused, currentSessionType)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseResumeIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply {
                action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ProgressMate: ${currentSessionType.name}")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pauseResumeIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification("ProgressMate", content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live focus and rest countdowns"
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "progressmate_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_EXTEND_5 = "ACTION_EXTEND_5"

        const val EXTRA_DURATION_SEC = "EXTRA_DURATION_SEC"
        const val EXTRA_TYPE = "EXTRA_TYPE"

        data class TimerState(
            val remainingSeconds: Long = 0,
            val elapsedSeconds: Long = 0,
            val isRunning: Boolean = false,
            val isPaused: Boolean = false,
            val sessionType: SessionType = SessionType.WORK
        )

        private val _timerFlow = MutableStateFlow(TimerState())
        val timerFlow: StateFlow<TimerState> = _timerFlow
    }
}
