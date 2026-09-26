package com.nj031.onetask.data.task

import android.util.Log
import com.nj031.onetask.data.sync.CloudBackupRepository
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// TEMPORARY DIAGNOSTIC INSTRUMENTATION (last-task-deletion-reappears investigation) - every Log.d
// call tagged with this constant, here and in HomeViewModel/HomeScreen, exists solely to build a
// timestamped causal chain from a live device repro, since this sandbox has no emulator/device to
// reproduce or observe runtime Flow/Compose behavior directly. Remove every call site tagged with
// this constant (and this constant itself) once the investigation concludes - none of it changes
// any functional behavior.
private const val DELETE_DEBUG_TAG = "ONE_TASK_DELETE_DEBUG"

// TEMPORARY DIAGNOSTIC LOG - see DELETE_DEBUG_TAG's own doc comment. Called immediately before
// every dao.upsert() call site in this file, so a live repro can show whether any of them fire
// for a task id after that same id's own DELETE_END - purely observational (a log line only),
// changes no behavior, no query, no persisted data.
private fun logUpsert(caller: String, task: TaskEntity) {
    Log.d(
        DELETE_DEBUG_TAG,
        "UPSERT_CALL caller=$caller taskId=${task.id} name=${task.name} ts=${System.currentTimeMillis()}"
    )
}

class TaskRepository(private val dao: TaskDao) {
    /**
     * Every task actually due on [date]: real rows stored with that exact date that are actually
     * due on it (see [TaskEntity.isDueOn] - a plain one-time task or an already-materialized
     * occurrence always qualifies, but a recurring series' own row only counts on its start date
     * if that date itself satisfies the series' own pattern) plus a virtual (not-yet-persisted)
     * occurrence for every OTHER active recurring series whose pattern matches [date] and hasn't
     * been materialized yet AND hasn't been explicitly deleted on this date (see
     * [RecurringExclusionEntity] - without this check, deleting a single occurrence would
     * otherwise be regenerated right back by this same combine on its very next emission, since
     * the series definition itself is untouched and still matches [date]). Repeat is evaluated
     * here, against the date actually being viewed, rather than being a label stored once on the
     * task.
     */
    fun observeTasksByDate(date: Long): Flow<List<TaskEntity>> =
        combine(
            dao.getByDate(date),
            dao.getActiveRecurringSeries(),
            dao.getExcludedSeriesIdsForDate(date)
        ) { exactMatches, series, excludedSeriesIds ->
            val dueExactMatches = exactMatches.filter { it.isDueOn(date) }
            val existingIds = dueExactMatches.mapTo(HashSet()) { it.id }
            val excludedSet = excludedSeriesIds.toHashSet()
            val virtualOccurrences = series.mapNotNull { seriesTask ->
                if (seriesTask.date == date || !seriesTask.matchesRecurrenceOn(date)) return@mapNotNull null
                if (seriesTask.id in excludedSet) return@mapNotNull null
                val occurrence = seriesTask.asVirtualOccurrence(date)
                if (occurrence.id in existingIds) null else occurrence
            }
            val result = (dueExactMatches + virtualOccurrences).sortedBy { it.createdAt }
            // TEMPORARY DIAGNOSTIC LOG - see DELETE_DEBUG_TAG's own doc comment.
            Log.d(
                DELETE_DEBUG_TAG,
                "COMBINE_EMIT date=$date ids=${result.map { it.id }} count=${result.size} " +
                    "exactIds=${exactMatches.map { it.id }} seriesIds=${series.map { it.id }} " +
                    "excluded=$excludedSeriesIds ts=${System.currentTimeMillis()}"
            )
            result
        }

    /** Same recurring-aware matching as [observeTasksByDate], applied across a whole date range
     * for the calendar's per-date task indicator dot - a date only shows a dot if it has a real
     * task actually due on it or is due for an active recurring series, without needing every
     * future occurrence to already be a persisted row. */
    fun observeDatesWithTasksBetween(startDate: Long, endDate: Long): Flow<List<Long>> =
        combine(dao.getTasksWithDateBetween(startDate, endDate), dao.getActiveRecurringSeries()) { tasksInRange, series ->
            val realDates = tasksInRange.filter { it.isDueOn(it.date) }.map { it.date }
            val recurringDates = (startDate..endDate).filter { day ->
                series.any { it.date != day && it.matchesRecurrenceOn(day) }
            }
            (realDates + recurringDates).distinct()
        }

    fun observeTaskById(id: String): Flow<TaskEntity?> = dao.getById(id)

    fun observeCustomTags(): Flow<List<String>> = dao.getCustomTags()

    /** Custom Categories only - see [DefaultCategory] for the 5 fixed ones, which never need a
     * Room row/Flow of their own since they can't be created, renamed, or deleted. Newest-first
     * (see [TaskDao.getCustomCategories]), per the Category spec. */
    fun observeCustomCategories(): Flow<List<CategoryEntity>> = dao.getCustomCategories()

    suspend fun getAllTasksOnce(): List<TaskEntity> = dao.getAll()

    suspend fun getAllCustomTagsOnce(): List<String> = dao.getCustomTagsOnce()

    suspend fun getAllCustomCategoriesOnce(): List<CategoryEntity> = dao.getCustomCategoriesOnce()

    suspend fun createTask(
        name: String,
        subtasks: List<Subtask>,
        timerMinutes: Int?,
        date: Long,
        priority: TaskPriority,
        reminderMinuteOfDay: Int?,
        reminderEpochDay: Long?,
        repeat: TaskRepeat,
        repeatDays: Set<DayOfWeek>,
        tag: String?,
        categoryId: String? = null,
        postponeIfIncomplete: Boolean,
        successCondition: SuccessCondition = SuccessCondition.ALL,
        successConditionThreshold: Int? = null
    ): TaskEntity {
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            name = name,
            subtasks = subtasks,
            timerMinutes = timerMinutes,
            date = date,
            priority = priority,
            reminderMinuteOfDay = reminderMinuteOfDay,
            reminderEpochDay = reminderEpochDay,
            repeat = repeat,
            repeatDays = repeatDays.toRepeatDaysString(),
            tag = tag,
            categoryId = categoryId,
            postponeIfIncomplete = postponeIfIncomplete,
            successCondition = successCondition,
            successConditionThreshold = successConditionThreshold,
            createdAt = now,
            updatedAt = now,
            // Negated so ascending-by-orderInAll sort (unchanged - see HomeScreen/reorderTasks)
            // places the newest task first without touching the sort direction itself, which
            // would otherwise invert the position of every task a user has already manually
            // drag-reordered (reorderTasks writes small sequential indices, not timestamps).
            orderInAll = -now
        ).reconcileStatusWithSuccessCondition()
        dao.insert(task)
        CloudBackupRepository.pushTask(task)
        return task
    }

    suspend fun updateTask(
        task: TaskEntity,
        name: String,
        subtasks: List<Subtask>,
        timerMinutes: Int?,
        date: Long,
        priority: TaskPriority,
        reminderMinuteOfDay: Int?,
        reminderEpochDay: Long?,
        repeat: TaskRepeat,
        repeatDays: Set<DayOfWeek>,
        tag: String?,
        categoryId: String? = task.categoryId,
        postponeIfIncomplete: Boolean,
        successCondition: SuccessCondition = task.successCondition,
        successConditionThreshold: Int? = task.successConditionThreshold
    ): TaskEntity {
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
            priority = priority,
            reminderMinuteOfDay = reminderMinuteOfDay,
            reminderEpochDay = reminderEpochDay,
            repeat = repeat,
            repeatDays = repeatDays.toRepeatDaysString(),
            tag = tag,
            categoryId = categoryId,
            postponeIfIncomplete = postponeIfIncomplete,
            successCondition = successCondition,
            successConditionThreshold = successConditionThreshold,
            timerEndAtMillis = newTimerEndAtMillis,
            timerRemainingMillis = newTimerRemainingMillis,
            updatedAt = now
        ).reconcileStatusWithSuccessCondition()
        logUpsert("updateTask", updated)
        dao.upsert(updated)
        CloudBackupRepository.pushTask(updated)
        return updated
    }

    /**
     * Toggles one subtask and re-syncs the parent task's own status with its Success Condition
     * (see [reconcileStatusWithSuccessCondition]) - e.g. checking off the last subtask an "All
     * tasks" condition needs auto-completes the parent, and later un-checking one auto-reopens
     * it. A no-op for a 0-subtask task's own completion, which the manual checkbox alone still
     * drives, exactly as before Success Condition existed.
     */
    suspend fun toggleSubtask(task: TaskEntity, subtaskId: String): TaskEntity {
        val updatedSubtasks = task.subtasks.map { subtask ->
            if (subtask.id == subtaskId) subtask.copy(completed = !subtask.completed) else subtask
        }
        val updated = task.copy(subtasks = updatedSubtasks, updatedAt = System.currentTimeMillis())
            .reconcileStatusWithSuccessCondition()
        logUpsert("toggleSubtask", updated)
        dao.upsert(updated)
        CloudBackupRepository.pushTask(updated)
        return updated
    }

    /** Marks the task Done, pausing (not resetting) any active timer so its progress is preserved. */
    suspend fun markTaskDone(task: TaskEntity): TaskEntity {
        val updated = task.markDoneTransition()
        logUpsert("markTaskDone", updated)
        dao.upsert(updated)
        CloudBackupRepository.pushTask(updated)
        return updated
    }

    /**
     * Deleting a recurring series' own definition row also deletes every occurrence already
     * materialized from it (see [TaskEntity.asVirtualOccurrence]) and every per-date exclusion
     * recorded against it, so a deleted recurring task doesn't leave individual-date leftovers
     * behind. Deleting a single occurrence of a still-active series (materialized or not) records
     * a [RecurringExclusionEntity] for that exact (series, date) pair - removing the row alone
     * isn't enough, since the series definition itself is still active and would otherwise
     * regenerate a fresh virtual occurrence for that date on the very next
     * [observeTasksByDate] emission, making the deletion appear to silently undo itself.
     * Deleting a plain, non-recurring task removes that one row, unchanged from before.
     *
     * Both recurring branches now delegate their local Room writes to a single [TaskDao]
     * `@Transaction` method ([TaskDao.deleteRecurringSeriesLocally]/
     * [TaskDao.deleteRecurringOccurrenceLocally]) instead of issuing them as separate suspend
     * calls: issuing them separately let a concurrent [observeTasksByDate] re-query land in
     * between two of them and observe a partially-applied delete (e.g. the series still reported
     * active by one flow with nothing yet excluding this date, while another flow had already
     * dropped the row) - regenerating the very occurrence just deleted, visible as the task
     * disappearing and then flashing back. Bundling each branch's local writes into one
     * transaction closes that window entirely, for every delete path, not just the one this file's
     * own git history shows was previously patched this way. Cloud sync (each awaited Firestore
     * call, and its relative order) is unchanged - only the local writes that precede it are now
     * atomic.
     */
    suspend fun deleteTask(task: TaskEntity) {
        // TEMPORARY DIAGNOSTIC LOG - see DELETE_DEBUG_TAG's own doc comment.
        Log.d(
            DELETE_DEBUG_TAG,
            "DELETE_START taskId=${task.id} name=${task.name} date=${task.date} " +
                "seriesId=${task.seriesId} repeat=${task.repeat} ts=${System.currentTimeMillis()}"
        )
        when {
            task.seriesId == null && task.repeat != TaskRepeat.NONE -> {
                Log.d(DELETE_DEBUG_TAG, "DELETE_BRANCH=SERIES taskId=${task.id} ts=${System.currentTimeMillis()}")
                val occurrenceIds = dao.deleteRecurringSeriesLocally(task)
                Log.d(DELETE_DEBUG_TAG, "DELETE_LOCAL_DONE=SERIES taskId=${task.id} ts=${System.currentTimeMillis()}")
                occurrenceIds.forEach { CloudBackupRepository.deleteTask(it) }
                CloudBackupRepository.deleteTask(task.id)
                CloudBackupRepository.deleteRecurringExclusionsForSeries(task.id)
            }
            task.seriesId != null -> {
                Log.d(DELETE_DEBUG_TAG, "DELETE_BRANCH=OCCURRENCE taskId=${task.id} ts=${System.currentTimeMillis()}")
                dao.deleteRecurringOccurrenceLocally(task, task.seriesId, task.date)
                Log.d(DELETE_DEBUG_TAG, "DELETE_LOCAL_DONE=OCCURRENCE taskId=${task.id} ts=${System.currentTimeMillis()}")
                CloudBackupRepository.deleteTask(task.id)
                CloudBackupRepository.pushRecurringExclusion(task.seriesId, task.date)
            }
            else -> {
                Log.d(DELETE_DEBUG_TAG, "DELETE_BRANCH=PLAIN taskId=${task.id} ts=${System.currentTimeMillis()}")
                dao.delete(task)
                Log.d(DELETE_DEBUG_TAG, "DELETE_LOCAL_DONE=PLAIN taskId=${task.id} ts=${System.currentTimeMillis()}")
                CloudBackupRepository.deleteTask(task.id)
            }
        }
        Log.d(DELETE_DEBUG_TAG, "DELETE_END taskId=${task.id} ts=${System.currentTimeMillis()}")
    }

    suspend fun addCustomTag(name: String) {
        dao.insertTag(TaskTagEntity(name = name))
        CloudBackupRepository.pushTag(name)
    }

    /** Removes the tag from the available custom-tags list only - deliberately never touches
     * the tasks table, so any existing task that already used this tag keeps displaying it
     * exactly as before; it just stops being offered for new/edited tasks going forward. */
    suspend fun deleteCustomTag(name: String) {
        dao.deleteTag(name)
        CloudBackupRepository.deleteTag(name)
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
        tags.forEach { tag ->
            dao.insertTag(TaskTagEntity(name = tag))
            CloudBackupRepository.pushTag(tag)
        }
    }

    /** Permanently deletes every task, custom tag, and custom category, locally and from the
     * cloud backup. Does not touch the account itself. */
    suspend fun deleteAllTasksAndTags() {
        val allTasks = dao.getAll()
        val allTags = dao.getCustomTagsOnce()
        val allCategories = dao.getCustomCategoriesOnce()
        dao.deleteAllTasks()
        dao.deleteAllTags()
        dao.deleteAllCategories()
        allTasks.forEach { CloudBackupRepository.deleteTask(it.id) }
        allTags.forEach { CloudBackupRepository.deleteTag(it) }
        allCategories.forEach { CloudBackupRepository.deleteCategory(it.id) }
    }

    suspend fun addCustomCategory(name: String): CategoryEntity {
        val category = CategoryEntity(name = name)
        dao.insertCategory(category)
        CloudBackupRepository.pushCategory(category)
        return category
    }

    /** Renames a custom category in place (same [CategoryEntity.id]) - every task already
     * referencing it by id (see [TaskEntity.categoryId]) picks up the new name automatically,
     * without needing any of those task rows to be touched. */
    suspend fun renameCustomCategory(id: String, name: String) {
        dao.renameCategory(id, name)
        val updated = dao.getCategoryById(id) ?: return
        CloudBackupRepository.pushCategory(updated)
    }

    /** Deletes a custom category entirely and, per the Category spec's delete-confirmation
     * behavior, turns every task that had it into "No Category" (never reassigned to anything
     * else) rather than leaving them referencing an id that no longer exists - both locally and
     * in each affected task's own cloud copy, so a later sign-in/reinstall doesn't resurrect the
     * stale reference. A category later created with the same name gets a brand-new id (see
     * [CategoryEntity]), so these tasks never silently reconnect to it. */
    suspend fun deleteCustomCategory(id: String) {
        val affectedTasks = dao.getTasksByCategoryId(id)
        dao.clearCategoryFromTasks(id)
        dao.deleteCategory(id)
        CloudBackupRepository.deleteCategory(id)
        affectedTasks.forEach { task -> CloudBackupRepository.pushTask(task.copy(categoryId = null)) }
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
        logUpsert("startTimer", updated)
        dao.upsert(updated)
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
        logUpsert("pauseTimer", updated)
        dao.upsert(updated)
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
        logUpsert("resetTimer", updated)
        dao.upsert(updated)
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
        logUpsert("finishTimer", updated)
        dao.upsert(updated)
        CloudBackupRepository.pushTask(updated)
    }

    /**
     * Restores a task that was manually marked Done back to its active section. A timed task
     * that had genuine progress (started at least once) resumes as a paused IN_PROGRESS timer
     * with its preserved remaining time, rather than being wiped back to a fresh Not Started
     * state; a task with no timer, or a timer that was never started, simply returns to Not
     * Started. Never auto-starts the timer either way.
     */
    suspend fun uncompleteTask(task: TaskEntity): TaskEntity {
        val updated = task.uncompleteTransition()
        logUpsert("uncompleteTask", updated)
        dao.upsert(updated)
        CloudBackupRepository.pushTask(updated)
        return updated
    }

    /**
     * Persists a drag-and-drop reorder: [orderedTasks] is the full task list for [scope]'s tab
     * (e.g. every task currently shown in In Progress) in its new order. Only that scope's own
     * order column is touched - reordering In Progress never writes orderInAll/orderInDone - so
     * each tab's manual order stays completely independent, and nothing about a task's status,
     * timer, or any other field changes.
     *
     * [orderedTasks] is a snapshot frozen at the moment the drag ended (see HomeScreen's own
     * onDragEnd), and this loop persists it sequentially, awaiting a cloud push per item before
     * moving to the next - for a list of any size this can take long enough that a task in it
     * gets deleted (by a separate, concurrent action) before this loop reaches that task's own
     * turn. [TaskDao.upsertUnlessDeletedSince] is what makes that safe: for a task this loop
     * already knew had a real row when it started (see [idsWithRealRowAtStart] below), it
     * re-checks - atomically, right before writing - whether that row is still there, and skips
     * persisting (locally and to the cloud) if it isn't, rather than letting `upsert`'s
     * INSERT-OR-REPLACE silently resurrect a task someone else just deleted. A task that never
     * had a row yet (a still-virtual recurring occurrence - see [TaskEntity.asVirtualOccurrence])
     * is unaffected: it keeps materializing on this reorder exactly as it already did before this
     * check existed.
     */
    suspend fun reorderTasks(scope: TaskOrderScope, orderedTasks: List<TaskEntity>) {
        val now = System.currentTimeMillis()
        val idsWithRealRowAtStart = dao.getExistingIds(orderedTasks.map { it.id }).toHashSet()
        // TEMPORARY DIAGNOSTIC LOG - see DELETE_DEBUG_TAG's own doc comment. Marks the whole
        // snapshot this loop is about to persist from, and its size, so a live repro can show
        // whether a later item's turn in this sequential loop still lands after some OTHER
        // task's delete (this list was frozen at drag-end time, before any such delete).
        Log.d(
            DELETE_DEBUG_TAG,
            "REORDER_START scope=$scope ids=${orderedTasks.map { it.id }} ts=${System.currentTimeMillis()}"
        )
        orderedTasks.forEachIndexed { index, task ->
            val updated = when (scope) {
                TaskOrderScope.ALL -> task.copy(orderInAll = index.toLong(), updatedAt = now)
                TaskOrderScope.IN_PROGRESS -> task.copy(orderInProgress = index.toLong(), updatedAt = now)
                TaskOrderScope.DONE -> task.copy(orderInDone = index.toLong(), updatedAt = now)
            }
            logUpsert("reorderTasks[index=$index/${orderedTasks.size}]", updated)
            val persisted = dao.upsertUnlessDeletedSince(task.id, task.id in idsWithRealRowAtStart, updated)
            if (persisted) {
                CloudBackupRepository.pushTask(updated)
            } else {
                // TEMPORARY DIAGNOSTIC LOG - see DELETE_DEBUG_TAG's own doc comment.
                Log.d(
                    DELETE_DEBUG_TAG,
                    "REORDER_SKIPPED_DELETED taskId=${task.id} ts=${System.currentTimeMillis()}"
                )
            }
        }
        Log.d(DELETE_DEBUG_TAG, "REORDER_END scope=$scope ts=${System.currentTimeMillis()}")
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/** Marks [this] Done, pausing (not resetting) any active timer so its progress is preserved -
 * the pure transition [TaskRepository.markTaskDone] and [reconcileStatusWithSuccessCondition]'s
 * own auto-completion both apply before persisting. */
private fun TaskEntity.markDoneTransition(): TaskEntity {
    val now = System.currentTimeMillis()
    val remainingMillis = timerEndAtMillis?.let { (it - now).coerceAtLeast(0) } ?: timerRemainingMillis
    return copy(status = TaskStatus.COMPLETED, timerEndAtMillis = null, timerRemainingMillis = remainingMillis, updatedAt = now)
}

/** Restores [this] from Done back to an active state - a timed task that had genuine progress
 * resumes as a paused IN_PROGRESS timer with its preserved remaining time, a task with no timer
 * or no progress simply returns to Not Started; never auto-starts the timer either way. The pure
 * transition [TaskRepository.uncompleteTask] and [reconcileStatusWithSuccessCondition]'s own
 * auto-uncompletion both apply before persisting. */
private fun TaskEntity.uncompleteTransition(): TaskEntity {
    val hasPreservedTimerProgress = timerMinutes != null && timerRemainingMillis != null
    val newStatus = if (hasPreservedTimerProgress) TaskStatus.IN_PROGRESS else TaskStatus.NOT_STARTED
    return copy(status = newStatus, updatedAt = System.currentTimeMillis())
}

/**
 * Re-syncs [TaskEntity.status] with [TaskEntity.successCondition] after anything that could have
 * changed subtasks, the condition, or the threshold (a subtask toggle, or a task edit) - so the
 * invariant "complete iff the condition is satisfied" holds continuously for a task that has
 * subtasks, not just right after the interaction that most recently changed it. A no-op for a
 * 0-subtask task ([TaskEntity.successConditionAppliesHere] false): its completion is driven
 * solely by its own manual checkbox, exactly as before Success Condition existed.
 */
private fun TaskEntity.reconcileStatusWithSuccessCondition(): TaskEntity {
    if (!successConditionAppliesHere()) return this
    val satisfied = isSuccessConditionSatisfied()
    return when {
        satisfied && status != TaskStatus.COMPLETED -> markDoneTransition()
        !satisfied && status == TaskStatus.COMPLETED -> uncompleteTransition()
        else -> this
    }
}
