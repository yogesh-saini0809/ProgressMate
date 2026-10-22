package com.progressmate.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.progressmate.app.data.AppDatabase
import com.progressmate.app.data.TaskEntity
import com.progressmate.app.motivation.AppLanguage
import com.progressmate.app.ui.screens.*
import com.progressmate.app.ui.theme.ProgressMateTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getInstance(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            ProgressMateTheme {
                val coroutineScope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf(0) }
                var language by remember { mutableStateOf(AppLanguage.HINGLISH) }

                val tasks by db.progressDao().observeRootTasks().collectAsState(initial = emptyList())

                val startOfDayMs = remember {
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }

                val todayFocusSec by db.progressDao().observeTodayFocusTimeSeconds(startOfDayMs)
                    .collectAsState(initial = 0L)

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == 0,
                                onClick = { currentScreen = 0 },
                                icon = { Icon(Icons.Default.CheckCircle, "Tasks") },
                                label = { Text("Goals") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == 1,
                                onClick = { currentScreen = 1 },
                                icon = { Icon(Icons.Default.HourglassTop, "Timer") },
                                label = { Text("Timer") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == 2,
                                onClick = { currentScreen = 2 },
                                icon = { Icon(Icons.Default.BarChart, "Progress") },
                                label = { Text("Stats") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == 3,
                                onClick = { currentScreen = 3 },
                                icon = { Icon(Icons.Default.Settings, "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        0 -> HomeScreen(
                            tasks = tasks,
                            todayFocusMinutes = (todayFocusSec ?: 0L) / 60,
                            onToggleTask = { task, isCompleted ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.progressDao().updateTask(
                                        task.copy(
                                            isCompleted = isCompleted,
                                            completedAtEpochMs = if (isCompleted) System.currentTimeMillis() else null
                                        )
                                    )
                                }
                            },
                            onAddTask = { title, notes ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.progressDao().insertTask(
                                        TaskEntity(title = title, notes = notes)
                                    )
                                }
                            },
                            onDeleteTask = { task ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.progressDao().deleteTaskWithSubtasks(task)
                                }
                            },
                            onStartQuickTimer = { currentScreen = 1 },
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> TimerScreen(modifier = Modifier.padding(innerPadding))
                        2 -> ProgressScreen(
                            weeklyFocusHours = listOf(1.5f, 2.0f, 0.8f, 3.2f, 2.5f, 4.0f, 1.2f),
                            totalFocusMinutes = (todayFocusSec ?: 0L) / 60,
                            totalCompletedTasks = tasks.count { it.isCompleted },
                            streakDays = 5,
                            modifier = Modifier.padding(innerPadding)
                        )
                        3 -> SettingsScreen(
                            currentLanguage = language,
                            onLanguageChange = { language = it },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
