package com.nj031.onetask.data.task

import androidx.room.TypeConverter

private const val SUBTASK_DELIMITER = ""
private const val SUBTASK_FIELD_DELIMITER = ""

class TaskConverters {
    @TypeConverter
    fun fromSubtasks(subtasks: List<Subtask>): String =
        subtasks.joinToString(SUBTASK_DELIMITER) { subtask ->
            listOf(subtask.id, subtask.name, subtask.completed.toString())
                .joinToString(SUBTASK_FIELD_DELIMITER)
        }

    @TypeConverter
    fun toSubtasks(value: String): List<Subtask> =
        if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(SUBTASK_DELIMITER).map { entry ->
                val parts = entry.split(SUBTASK_FIELD_DELIMITER)
                Subtask(id = parts[0], name = parts[1], completed = parts[2].toBoolean())
            }
        }

    @TypeConverter
    fun fromRepeat(repeat: TaskRepeat): String = repeat.name

    @TypeConverter
    fun toRepeat(value: String): TaskRepeat = TaskRepeat.valueOf(value)

    @TypeConverter
    fun fromPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): TaskPriority = TaskPriority.valueOf(value)

    @TypeConverter
    fun fromStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
}
