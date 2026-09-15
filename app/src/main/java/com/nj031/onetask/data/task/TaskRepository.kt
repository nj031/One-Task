package com.nj031.onetask.data.task

import com.nj031.onetask.data.sync.CloudBackupRepository
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    fun observeTasksByDate(date: Long): Flow<List<TaskEntity>> = dao.getByDate(date)

    fun observeTaskById(id: String): Flow<TaskEntity?> = dao.getById(id)

    fun observeCustomTags(): Flow<List<String>> = dao.getCustomTags()

    suspend fun createTask(
        name: String,
        subtasks: List<Subtask>,
        timerMinutes: Int?,
        date: Long,
        repeat: TaskRepeat,
        tag: String?,
        postponeIfIncomplete: Boolean
    ) {
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            name = name,
            subtasks = subtasks,
            timerMinutes = timerMinutes,
            date = date,
            repeat = repeat,
            tag = tag,
            postponeIfIncomplete = postponeIfIncomplete,
            createdAt = now,
            updatedAt = now
        )
        dao.insert(task)
        CloudBackupRepository.pushTask(task)
    }

    suspend fun updateTask(
        task: TaskEntity,
        name: String,
        subtasks: List<Subtask>,
        timerMinutes: Int?,
        date: Long,
        repeat: TaskRepeat,
        tag: String?,
        postponeIfIncomplete: Boolean
    ) {
        val now = System.currentTimeMillis()
        val newTotalMillis = (timerMinutes ?: 0) * MILLIS_PER_MINUTE

        // Editing the configured timer duration must never leave stale runtime state that
        // still targets the OLD duration. Remaining time is preserved as-is when it still
        // fits inside the new duration (e.g. 24:32 remaining carries over into a 60-minute
        // timer unchanged), but is clamped down to the new duration when it doesn't (e.g.
        // ~45 minutes remaining on a still-running 45-minute timer can't remain "45 minutes
        // remaining" once the timer is edited down to 25 minutes). A never-started timer has
        // no runtime state to adjust.
        val newTimerEndAtMillis: Long?
        val newTimerRemainingMillis: Long?
        when {
            timerMinutes == null -> {
                newTimerEndAtMillis = null
                newTimerRemainingMillis = null
            }
            task.timerEndAtMillis != null -> {
                val currentRemainingMillis = (task.timerEndAtMillis - now).coerceAtLeast(0)
                val clampedRemainingMillis = currentRemainingMillis.coerceAtMost(newTotalMillis)
                newTimerEndAtMillis = now + clampedRemainingMillis
                newTimerRemainingMillis = null
            }
            task.timerRemainingMillis != null -> {
                newTimerEndAtMillis = null
                newTimerRemainingMillis = task.timerRemainingMillis.coerceAtMost(newTotalMillis)
            }
            else -> {
                newTimerEndAtMillis = null
                newTimerRemainingMillis = null
            }
        }

        val updated = task.copy(
            name = name,
            subtasks = subtasks,
            timerMinutes = timerMinutes,
            date = date,
            repeat = repeat,
            tag = tag,
            postponeIfIncomplete = postponeIfIncomplete,
            timerEndAtMillis = newTimerEndAtMillis,
            timerRemainingMillis = newTimerRemainingMillis,
            updatedAt = now
        )
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    suspend fun setStatus(task: TaskEntity, status: TaskStatus) {
        val updated = task.copy(status = status, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    suspend fun toggleSubtask(task: TaskEntity, subtaskId: String) {
        val updatedSubtasks = task.subtasks.map { subtask ->
            if (subtask.id == subtaskId) subtask.copy(completed = !subtask.completed) else subtask
        }
        val updated = task.copy(subtasks = updatedSubtasks, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    /** Marks the task Done, pausing (not resetting) any active timer so its progress is preserved. */
    suspend fun markTaskDone(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val remainingMillis = task.timerEndAtMillis?.let { (it - now).coerceAtLeast(0) }
            ?: task.timerRemainingMillis
        val updated = task.copy(
            status = TaskStatus.COMPLETED,
            timerEndAtMillis = null,
            timerRemainingMillis = remainingMillis,
            updatedAt = now
        )
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    suspend fun deleteTask(task: TaskEntity) {
        dao.delete(task)
        CloudBackupRepository.deleteTask(task.id)
    }

    suspend fun addCustomTag(name: String) {
        dao.insertTag(TaskTagEntity(name = name))
    }

    suspend fun postponeOverdueTasks(today: Long) {
        dao.postponeOverdueTasks(today, System.currentTimeMillis())
    }

    /** Starts a fresh timer, or resumes one from its frozen remaining duration. */
    suspend fun startTimer(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val remainingMillis = task.timerRemainingMillis ?: ((task.timerMinutes ?: 0) * MILLIS_PER_MINUTE)
        val updated = task.copy(
            status = TaskStatus.IN_PROGRESS,
            timerEndAtMillis = now + remainingMillis,
            timerRemainingMillis = null,
            updatedAt = now
        )
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    suspend fun pauseTimer(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val remainingMillis = task.timerEndAtMillis?.let { (it - now).coerceAtLeast(0) }
            ?: task.timerRemainingMillis
            ?: 0L
        val updated = task.copy(
            timerEndAtMillis = null,
            timerRemainingMillis = remainingMillis,
            updatedAt = now
        )
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    suspend fun resetTimer(task: TaskEntity) {
        val totalMillis = (task.timerMinutes ?: 0) * MILLIS_PER_MINUTE
        val updated = task.copy(
            timerEndAtMillis = null,
            timerRemainingMillis = totalMillis,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    suspend fun completeTimer(task: TaskEntity) {
        val updated = task.copy(
            status = TaskStatus.COMPLETED,
            timerEndAtMillis = null,
            timerRemainingMillis = 0L,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
