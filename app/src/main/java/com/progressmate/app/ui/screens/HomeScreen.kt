package com.progressmate.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.progressmate.app.data.TaskEntity
import com.progressmate.app.motivation.AppLanguage
import com.progressmate.app.motivation.MotivationManager
import com.progressmate.app.ui.components.CircularProgressBadge
import com.progressmate.app.ui.components.TaskItemCard
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    tasks: List<TaskEntity>,
    todayFocusMinutes: Long,
    onToggleTask: (TaskEntity, Boolean) -> Unit,
    onAddTask: (String, String) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onStartQuickTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddModal by remember { mutableStateOf(false) }
    var encouragementText by remember { mutableStateOf("Keep up your rhythm!") }

    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val progress = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()

    val formattedDate = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Welcome back!",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = encouragementText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CircularProgressBadge(
                            percentage = progress,
                            completedCount = completedTasks,
                            totalCount = totalTasks
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Focused Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${todayFocusMinutes}m",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Button(
                                onClick = onStartQuickTimer,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Focus Now")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Today's Priorities",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(tasks, key = { it.id }) { task ->
                TaskItemCard(
                    task = task,
                    onCheckedChange = { isChecked ->
                        onToggleTask(task, isChecked)
                        if (isChecked) {
                            MotivationManager.triggerHapticFeedback(context)
                            encouragementText = MotivationManager.getRandomCheer(AppLanguage.HINGLISH)
                        }
                    },
                    onDelete = { onDeleteTask(task) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddModal) {
        QuickAddTaskDialog(
            onDismiss = { showAddModal = false },
            onConfirm = { title, note ->
                onAddTask(title, note)
                showAddModal = false
            }
        )
    }
}

@Composable
fun QuickAddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Action Step") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What needs to be done?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Sub-step or note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, notes) },
                enabled = title.isNotBlank()
            ) {
                Text("Add Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
