package com.nj031.onetask.data.task

import androidx.room.TypeConverter

private const val SUBTASK_DELIMITER = ""

class TaskConverters {
    @TypeConverter
    fun fromSubtasks(subtasks: List<String>): String = subtasks.joinToString(SUBTASK_DELIMITER)

    @TypeConverter
    fun toSubtasks(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SUBTASK_DELIMITER)

    @TypeConverter
    fun fromRepeat(repeat: TaskRepeat): String = repeat.name

    @TypeConverter
    fun toRepeat(value: String): TaskRepeat = TaskRepeat.valueOf(value)

    @TypeConverter
    fun fromStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
}
