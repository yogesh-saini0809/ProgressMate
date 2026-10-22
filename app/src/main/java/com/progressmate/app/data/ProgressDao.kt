package com.progressmate.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM tasks WHERE parentTaskId IS NULL ORDER BY isCompleted ASC, orderIndex ASC, id DESC")
    fun observeRootTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId ORDER BY isCompleted ASC, id ASC")
    fun observeSubtasks(parentId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, id DESC LIMIT 1")
    suspend fun getTopUnfinishedTask(): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE parentTaskId = :parentId")
    suspend fun deleteSubtasks(parentId: Long)

    @Transaction
    suspend fun deleteTaskWithSubtasks(task: TaskEntity) {
        deleteSubtasks(task.id)
        deleteTask(task)
    }

    @Insert
    suspend fun insertSession(session: WorkSessionEntity): Long

    @Query("SELECT * FROM work_sessions WHERE startTimeEpochMs >= :startOfDayMs ORDER BY startTimeEpochMs DESC")
    fun observeTodaySessions(startOfDayMs: Long): Flow<List<WorkSessionEntity>>

    @Query("SELECT SUM(durationSeconds) FROM work_sessions WHERE sessionType = 'WORK' AND startTimeEpochMs >= :startOfDayMs")
    fun observeTodayFocusTimeSeconds(startOfDayMs: Long): Flow<Long?>
}
