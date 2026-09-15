package com.nj031.onetask.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskStatus { NOT_STARTED, COMPLETED }

enum class TaskRepeat { NONE, DAILY, WEEKLY, MONTHLY }

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subtasks: List<String> = emptyList(),
    val timerMinutes: Int? = null,
    val date: Long,
    val repeat: TaskRepeat = TaskRepeat.NONE,
    val tag: String? = null,
    val postponeIfIncomplete: Boolean = true,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "task_tags")
data class TaskTagEntity(
    @PrimaryKey val name: String
)
