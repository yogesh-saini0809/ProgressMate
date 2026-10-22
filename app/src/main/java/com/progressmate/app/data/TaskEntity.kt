package com.progressmate.app.data

import androidx.room.*

enum class Priority { LOW, MEDIUM, HIGH }
enum class SessionType { WORK, SHORT_BREAK, LONG_BREAK }

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val priority: Priority = Priority.MEDIUM,
    val deadlineEpochMs: Long? = null,
    val isCompleted: Boolean = false,
    val parentTaskId: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null,
    val orderIndex: Int = 0
)

@Entity(tableName = "work_sessions")
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val durationSeconds: Long,
    val sessionType: SessionType,
    val linkedTaskId: Long? = null
)
