package com.nj031.onetask.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

enum class TaskRepeat { NONE, DAILY, SELECT_DAYS }

/** NONE is the default "no priority selected" state - not a fourth visible priority level, just
 * the absence of one. The Add Task screen only ever offers SMALL/MEDIUM/HIGH as selectable
 * chips. */
enum class TaskPriority { NONE, SMALL, MEDIUM, HIGH }

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
    val priority: TaskPriority = TaskPriority.NONE,
    val repeat: TaskRepeat = TaskRepeat.NONE,
    // Only meaningful when repeat == SELECT_DAYS: comma-separated ISO day-of-week numbers
    // (1=Monday..7=Sunday, see DayOfWeek.getValue) - see repeatDaysSet()/toRepeatDaysString()
    // below, the same flattened-string approach TaskConverters already uses for subtasks.
    val repeatDays: String = "",
    // Null for a plain one-time task, or for a recurring series' own definition row (whose
    // `date` is the series' start date and `repeat` is DAILY/SELECT_DAYS). Set to that series
    // row's id for a materialized occurrence on some other date - see asVirtualOccurrence()
    // and TaskRepository.observeTasksByDate(). An occurrence's own `repeat`/`repeatDays` are
    // copied from the series purely for display; seriesId (not repeat) is what excludes it from
    // being treated as a series definition in its own right.
    val seriesId: String? = null,
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

private const val REPEAT_DAYS_DELIMITER = ","

/** Marks a synthetic id/suffix for a not-yet-persisted recurring occurrence - see
 * [asVirtualOccurrence]. Purely descriptive (kept out of [TaskEntity.seriesId] equality/lookup
 * logic, which always uses the real series id instead), but keeps every generated id
 * self-explanatory if it ever surfaces in a log or a Firestore document list. */
private const val VIRTUAL_OCCURRENCE_ID_INFIX = "_occ_"

fun Set<DayOfWeek>.toRepeatDaysString(): String = joinToString(REPEAT_DAYS_DELIMITER) { it.value.toString() }

fun TaskEntity.repeatDaysSet(): Set<DayOfWeek> =
    repeatDays.split(REPEAT_DAYS_DELIMITER)
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .map(DayOfWeek::of)
        .toSet()

/**
 * Whether this recurring series (a row with [TaskEntity.seriesId] == null and
 * [TaskEntity.repeat] != NONE) is due on [targetEpochDay] - never before its own start date
 * ([TaskEntity.date]), and for SELECT_DAYS, only on a date whose weekday is in
 * [repeatDaysSet]. Used both for the series' own start date (via [isDueOn]) and for every
 * other date it might also be due on (via [asVirtualOccurrence]'s caller).
 */
fun TaskEntity.matchesRecurrenceOn(targetEpochDay: Long): Boolean {
    if (targetEpochDay < date) return false
    return when (repeat) {
        TaskRepeat.NONE -> false
        TaskRepeat.DAILY -> true
        TaskRepeat.SELECT_DAYS -> LocalDate.ofEpochDay(targetEpochDay).dayOfWeek in repeatDaysSet()
    }
}

/**
 * Whether a row whose own [TaskEntity.date] already equals [date] should actually be shown on
 * it. True unconditionally for a plain one-time task ([TaskEntity.repeat] == NONE) or an
 * already-materialized occurrence ([TaskEntity.seriesId] != null) - each addresses one specific
 * date on its own terms, exactly like the pre-Repeat-fix behavior. For a recurring series' own
 * definition row, though, its literal `date` is just the series' *start* date, not a guarantee
 * that date itself satisfies the series' own pattern - e.g. a Select Days series created on a
 * Friday that doesn't include Friday must not show on Friday merely because that's the row's
 * stored date, so this defers to [matchesRecurrenceOn] instead of assuming a match.
 */
fun TaskEntity.isDueOn(date: Long): Boolean =
    seriesId != null || repeat == TaskRepeat.NONE || matchesRecurrenceOn(date)

/**
 * A not-yet-persisted occurrence of this recurring series on [targetEpochDay], shown exactly
 * like a real task until the user actually interacts with it (toggling completion, editing it,
 * starting its timer, ...) - see [TaskDao.upsert] - at which point that interaction persists this
 * exact shape as a real row, independent from then on of every other occurrence and of the
 * series definition itself. The id is deterministic (same series + same date always produces the
 * same id) so a later real interaction, and this function's own dedup check against already-
 * materialized rows, always agree on which row a given date's occurrence is.
 */
fun TaskEntity.asVirtualOccurrence(targetEpochDay: Long): TaskEntity = copy(
    id = "$id$VIRTUAL_OCCURRENCE_ID_INFIX$targetEpochDay",
    subtasks = subtasks.map { it.copy(completed = false) },
    date = targetEpochDay,
    seriesId = id,
    status = TaskStatus.NOT_STARTED,
    timerEndAtMillis = null,
    timerRemainingMillis = null,
    orderInAll = createdAt,
    orderInProgress = createdAt,
    orderInDone = createdAt
)
