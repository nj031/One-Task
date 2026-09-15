package com.nj031.onetask.data.task

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
        dao.insert(
            TaskEntity(
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
        )
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
        dao.update(
            task.copy(
                name = name,
                subtasks = subtasks,
                timerMinutes = timerMinutes,
                date = date,
                repeat = repeat,
                tag = tag,
                postponeIfIncomplete = postponeIfIncomplete,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setStatus(task: TaskEntity, status: TaskStatus) {
        dao.update(task.copy(status = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleSubtask(task: TaskEntity, subtaskId: String) {
        val updatedSubtasks = task.subtasks.map { subtask ->
            if (subtask.id == subtaskId) subtask.copy(completed = !subtask.completed) else subtask
        }
        dao.update(task.copy(subtasks = updatedSubtasks, updatedAt = System.currentTimeMillis()))
    }

    /** Marks the task Done, pausing (not resetting) any active timer so its progress is preserved. */
    suspend fun markTaskDone(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val remainingMillis = task.timerEndAtMillis?.let { (it - now).coerceAtLeast(0) }
            ?: task.timerRemainingMillis
        dao.update(
            task.copy(
                status = TaskStatus.COMPLETED,
                timerEndAtMillis = null,
                timerRemainingMillis = remainingMillis,
                updatedAt = now
            )
        )
    }

    suspend fun deleteTask(task: TaskEntity) {
        dao.delete(task)
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
        dao.update(
            task.copy(
                status = TaskStatus.IN_PROGRESS,
                timerEndAtMillis = now + remainingMillis,
                timerRemainingMillis = null,
                updatedAt = now
            )
        )
    }

    suspend fun pauseTimer(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val remainingMillis = task.timerEndAtMillis?.let { (it - now).coerceAtLeast(0) }
            ?: task.timerRemainingMillis
            ?: 0L
        dao.update(
            task.copy(
                timerEndAtMillis = null,
                timerRemainingMillis = remainingMillis,
                updatedAt = now
            )
        )
    }

    suspend fun resetTimer(task: TaskEntity) {
        val totalMillis = (task.timerMinutes ?: 0) * MILLIS_PER_MINUTE
        dao.update(
            task.copy(
                timerEndAtMillis = null,
                timerRemainingMillis = totalMillis,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun completeTimer(task: TaskEntity) {
        dao.update(
            task.copy(
                status = TaskStatus.COMPLETED,
                timerEndAtMillis = null,
                timerRemainingMillis = 0L,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
