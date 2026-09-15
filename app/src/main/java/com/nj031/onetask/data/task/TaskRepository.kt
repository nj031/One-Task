package com.nj031.onetask.data.task

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    fun observeTasksByDate(date: Long): Flow<List<TaskEntity>> = dao.getByDate(date)

    fun observeCustomTags(): Flow<List<String>> = dao.getCustomTags()

    suspend fun createTask(
        name: String,
        subtasks: List<String>,
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
        subtasks: List<String>,
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

    suspend fun addCustomTag(name: String) {
        dao.insertTag(TaskTagEntity(name = name))
    }

    suspend fun postponeOverdueTasks(today: Long) {
        dao.postponeOverdueTasks(today, System.currentTimeMillis())
    }
}
