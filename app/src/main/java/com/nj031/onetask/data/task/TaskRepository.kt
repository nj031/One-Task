package com.nj031.onetask.data.task

import com.nj031.onetask.data.sync.CloudBackupRepository
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    fun observeTasksByDate(date: Long): Flow<List<TaskEntity>> = dao.getByDate(date)

    fun observeTaskById(id: String): Flow<TaskEntity?> = dao.getById(id)

    fun observeCustomTags(): Flow<List<String>> = dao.getCustomTags()

    suspend fun getAllTasksOnce(): List<TaskEntity> = dao.getAll()

    suspend fun getAllCustomTagsOnce(): List<String> = dao.getCustomTagsOnce()

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
        // still targets the OLD duration, and must never silently keep a timer counting down
        // against a duration the user hasn't confirmed running against. Remaining time is
        // preserved as-is when it still fits inside the new duration, but is clamped down to
        // the new duration when it doesn't (e.g. ~45 minutes remaining on a still-running
        // 45-minute timer can't remain "45 minutes remaining" once the timer is edited down to
        // 25 minutes). Editing a currently-RUNNING timer's duration also stops (pauses) it, so
        // the edit always lands on a stable, reviewable state rather than one still ticking
        // down against numbers the user just changed. A never-started timer has no runtime
        // state to adjust.
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
                newTimerEndAtMillis = null
                newTimerRemainingMillis = clampedRemainingMillis
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

    /** Upserts (by id) the given tasks/tags into local storage and mirrors the tasks to the
     * cloud backup, without touching any existing task/tag not present in [tasks]/[tags]. Used
     * by Data & Privacy's Restore flow, which merges a backup file into the current account
     * rather than replacing it. */
    suspend fun restoreTasks(tasks: List<TaskEntity>, tags: List<String>) {
        tasks.forEach { task ->
            dao.insert(task)
            CloudBackupRepository.pushTask(task)
        }
        tags.forEach { tag -> dao.insertTag(TaskTagEntity(name = tag)) }
    }

    /** Permanently deletes every task and custom tag, locally and from the cloud backup. Does
     * not touch the account itself. */
    suspend fun deleteAllTasksAndTags() {
        val allTasks = dao.getAll()
        dao.deleteAllTasks()
        dao.deleteAllTags()
        allTasks.forEach { CloudBackupRepository.deleteTask(it.id) }
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

    /** Restores the task's full configured duration, un-started and stopped. */
    suspend fun resetTimer(task: TaskEntity) {
        val totalMillis = (task.timerMinutes ?: 0) * MILLIS_PER_MINUTE
        val updated = task.copy(
            status = TaskStatus.NOT_STARTED,
            timerEndAtMillis = null,
            timerRemainingMillis = totalMillis,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    /**
     * Stops the timer at 0 when the countdown naturally reaches zero, WITHOUT deciding the
     * task's completion for the user: the task's status is left as-is (still IN_PROGRESS) so
     * Focus Mode can offer "Mark Task Done" / "Continue Task" rather than assuming every
     * finished session means every subtask is done too. Safe to call more than once (e.g. from
     * both the foreground service and the UI's own tick loop) - it always converges on the same
     * stopped-at-zero state.
     */
    suspend fun finishTimer(task: TaskEntity) {
        val updated = task.copy(
            timerEndAtMillis = null,
            timerRemainingMillis = 0L,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    /**
     * Restores a task that was manually marked Done back to its active section. A timed task
     * that had genuine progress (started at least once) resumes as a paused IN_PROGRESS timer
     * with its preserved remaining time, rather than being wiped back to a fresh Not Started
     * state; a task with no timer, or a timer that was never started, simply returns to Not
     * Started. Never auto-starts the timer either way.
     */
    suspend fun uncompleteTask(task: TaskEntity) {
        val hasPreservedTimerProgress = task.timerMinutes != null && task.timerRemainingMillis != null
        val newStatus = if (hasPreservedTimerProgress) TaskStatus.IN_PROGRESS else TaskStatus.NOT_STARTED
        val updated = task.copy(status = newStatus, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        CloudBackupRepository.pushTask(updated)
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
