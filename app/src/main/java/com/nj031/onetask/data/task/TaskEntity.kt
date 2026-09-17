package com.nj031.onetask.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

enum class TaskRepeat { NONE, DAILY, WEEKLY, MONTHLY }

/** Which tab's manual drag-and-drop order a [TaskRepository.reorderTasks] call updates - each
 * one is a completely independent ordering over the same tasks, per [TaskEntity]'s three order
 * columns below. */
enum class TaskOrderScope { ALL, IN_PROGRESS, DONE }

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subtasks: List<Subtask> = emptyList(),
    val timerMinutes: Int? = null,
    val date: Long,
    val repeat: TaskRepeat = TaskRepeat.NONE,
    val tag: String? = null,
    val postponeIfIncomplete: Boolean = true,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    // Non-null while the timer is actively running: the absolute wall-clock time
    // (System.currentTimeMillis()) at which the countdown reaches zero. Remaining
    // time is always derived as (timerEndAtMillis - now), never assumed from tick
    // count, so it stays correct across pauses, navigation, and process death.
    val timerEndAtMillis: Long? = null,
    // Frozen remaining duration while the timer is paused or has been reset.
    // Null means "never started" (full timerMinutes duration applies).
    val timerRemainingMillis: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    // Manual drag-and-drop order within each Tasks-homepage tab - kept as three separate
    // columns (rather than one shared order) so reordering in one tab, e.g. In Progress, never
    // touches how the same tasks are ordered in All or Done. Each defaults to createdAt, so a
    // task's position in a tab it has never been manually reordered within still falls back to
    // creation order - exactly how every tab already behaved before drag-and-drop existed.
    val orderInAll: Long = createdAt,
    val orderInProgress: Long = createdAt,
    val orderInDone: Long = createdAt
)

@Entity(tableName = "task_tags")
data class TaskTagEntity(
    @PrimaryKey val name: String
)
