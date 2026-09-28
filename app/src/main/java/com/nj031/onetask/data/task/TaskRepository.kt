package com.nj031.onetask.data.task

import com.nj031.onetask.data.sync.CloudBackupRepository
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** [TaskRepository.updateTask]'s result: the edited row itself, and - only when this edit
 * detached it from its old recurring series (see [TaskRepository.updateTask]'s own doc comment) -
 * that old series' own updated row, so the caller can recompute its one reminder against the new
 * boundary too. Null when this edit didn't touch any series membership at all. */
data class TaskUpdateResult(val updated: TaskEntity, val detachedFromSeries: TaskEntity?)

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
     *
     * The same exclusion set also applies to a recurring series' own definition row on its own
     * start date - see the Delete Behavior Contract's "this occurrence" choice
     * ([TaskDao.excludeSeriesOwnStartDateLocally]): that row is never deleted when only its start
     * date is deleted (deleting it would remove the series' pattern entirely, taking every later
     * date down with it), so this is the only place that particular exclusion can actually take
     * effect.
     */
    fun observeTasksByDate(date: Long): Flow<List<TaskEntity>> =
        combine(
            dao.getByDate(date),
            dao.getActiveRecurringSeries(),
            dao.getExcludedSeriesIdsForDate(date)
        ) { exactMatches, series, excludedSeriesIds ->
            val excludedSet = excludedSeriesIds.toHashSet()
            val dueExactMatches = exactMatches.filter { task ->
                task.isDueOn(date) &&
                    !(task.seriesId == null && task.repeat != TaskRepeat.NONE && task.id in excludedSet)
            }
            val existingIds = dueExactMatches.mapTo(HashSet()) { it.id }
            val virtualOccurrences = series.mapNotNull { seriesTask ->
                if (seriesTask.date == date || !seriesTask.matchesRecurrenceOn(date)) return@mapNotNull null
                if (seriesTask.id in excludedSet) return@mapNotNull null
                val occurrence = seriesTask.asVirtualOccurrence(date)
                if (occurrence.id in existingIds) null else occurrence
            }
            (dueExactMatches + virtualOccurrences).sortedBy { it.createdAt }
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

    /**
     * Persists an edit to [task]. Ordinary field edits (name, priority, subtasks, category, a
     * series' own reminder, ...) behave exactly as before - the row is simply updated in place.
     *
     * If [task] is itself a materialized occurrence of an active series ([TaskEntity.seriesId] !=
     * null) AND this edit changes its own [repeat]/[repeatDays] or [date], the Delete Behavior
     * Contract's edit rules apply instead: [task] detaches from its old series entirely (becomes
     * independent - [seriesId] cleared to null - so it's picked back up by [observeTasksByDate]/
     * [observeDatesWithTasksBetween] as its own plain task or, if [repeat] is non-NONE, the start
     * of a brand-new series from its own [date]) and the OLD series is capped so it never again
     * matches [task]'s ORIGINAL date or any date after it (see [TaskUpdateResult.detachedFromSeries]
     * - the caller should reschedule ITS one reminder too, since the boundary just moved). Editing
     * the series' own definition row directly (repeat == NONE turns it into a plain task, non-NONE
     * keeps/starts it as a series from its own date) never detaches anything - see
     * [TaskEntity.seriesId]'s own doc comment for why only an occurrence, never the definition row
     * itself, can be "detached".
     */
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
    ): TaskUpdateResult {
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

        val newRepeatDays = repeatDays.toRepeatDaysString()
        // Whether this edit moves [task] out from under its old series entirely, rather than
        // just changing one of its own already-independent fields - see this function's own doc
        // comment above for why only THESE two fields (never e.g. name/priority/subtasks) decide
        // it. [task] must already be a materialized occurrence (seriesId != null) - the series'
        // own definition row is never "detached" by editing it, since it, together with its
        // seriesId staying null, IS what keeps generating every later date's occurrence; editing
        // its own repeat/date already takes effect directly (see getActiveRecurringSeries).
        val detachesFromOldSeries = task.seriesId != null &&
            (repeat != task.repeat || newRepeatDays != task.repeatDays || date != task.date)

        val updated = task.copy(
            name = name,
            subtasks = subtasks,
            timerMinutes = timerMinutes,
            date = date,
            priority = priority,
            reminderMinuteOfDay = reminderMinuteOfDay,
            reminderEpochDay = reminderEpochDay,
            repeat = repeat,
            repeatDays = newRepeatDays,
            seriesId = if (detachesFromOldSeries) null else task.seriesId,
            tag = tag,
            categoryId = categoryId,
            postponeIfIncomplete = postponeIfIncomplete,
            successCondition = successCondition,
            successConditionThreshold = successConditionThreshold,
            timerEndAtMillis = newTimerEndAtMillis,
            timerRemainingMillis = newTimerRemainingMillis,
            updatedAt = now
        ).reconcileStatusWithSuccessCondition()
        dao.upsert(updated)
        CloudBackupRepository.pushTask(updated)

        // Cap the OLD series at [task]'s own ORIGINAL date (before this edit) so it stops
        // generating anything from there on - otherwise the old series would keep regenerating a
        // virtual occurrence right on top of [updated]'s new independent existence (if the date
        // didn't change) or would keep regenerating [task]'s old date after [updated] has moved
        // away from it (if it did) - see capRecurrenceAt's own doc comment, the same primitive
        // "this & future" delete uses. Done AFTER the upsert above, so its bulk prune can never
        // sweep up [updated] itself (already detached, seriesId now null, by the time this runs).
        val detachedFromSeries = if (detachesFromOldSeries) {
            capRecurrenceAndSync(task.seriesId!!, task.date)
        } else {
            null
        }
        return TaskUpdateResult(updated, detachedFromSeries)
    }

    /** Shared by the Delete Behavior Contract's "this & future" choice
     * ([deleteRecurringThisAndFuture]) and [updateTask]'s detach-from-old-series logic - see
     * [TaskDao.capRecurrenceAt]'s own doc comment. */
    private suspend fun capRecurrenceAndSync(seriesId: String, cutoffEpochDay: Long): TaskEntity? {
        val result = dao.capRecurrenceAt(seriesId, cutoffEpochDay) ?: return null
        result.prunedOccurrenceIds.forEach { CloudBackupRepository.deleteTask(it) }
        CloudBackupRepository.pushTask(result.updatedSeries)
        return result.updatedSeries
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
        dao.upsert(updated)
        CloudBackupRepository.pushTask(updated)
        return updated
    }

    /** Marks the task Done, pausing (not resetting) any active timer so its progress is preserved. */
    suspend fun markTaskDone(task: TaskEntity): TaskEntity {
        val updated = task.markDoneTransition()
        dao.upsert(updated)
        CloudBackupRepository.pushTask(updated)
        return updated
    }

    /**
     * The Delete Behavior Contract's "Delete this occurrence" choice for a recurring task - the
     * ONLY thing this ever removes is [task]'s own single date; every past occurrence, every
     * other future occurrence, and the series' own recurrence pattern are all left completely
     * untouched (there is deliberately no "delete entire series including past" operation - see
     * [deleteRecurringThisAndFuture] for the bounded, still-past-preserving alternative).
     *
     * [task] is either an ordinary occurrence (materialized or still-virtual - see
     * [TaskEntity.asVirtualOccurrence]) of an active series, or the series' own definition row
     * itself (its start date being the one selected) - the two need different local writes (the
     * definition row must never be deleted, since deleting it would take every later date's
     * occurrence down with it), so [TaskDao.deleteRecurringOccurrenceLocally] handles the former
     * and [TaskDao.excludeSeriesOwnStartDateLocally] the latter; both ultimately record the exact
     * same kind of [RecurringExclusionEntity] this app already uses to stop a deleted occurrence
     * from being regenerated (see [observeTasksByDate]'s own doc comment). The series' one
     * reminder (see ReminderManager) is deliberately left untouched either way - it belongs to the
     * whole series, not to any single date, and isn't affected by excluding one of them.
     */
    suspend fun deleteRecurringOccurrence(task: TaskEntity) {
        if (task.seriesId == null) {
            dao.excludeSeriesOwnStartDateLocally(task.id, task.date)
            CloudBackupRepository.pushRecurringExclusion(task.id, task.date)
            // The series row itself stays alive (it's still needed for later dates), but if it
            // currently carries a live timer for its own now-excluded start date, that timer must
            // stop - see TIMER + DELETE in the Delete Behavior Contract, and capRecurrenceAt's own
            // doc comment for why this is safe (every future occurrence starts fresh regardless).
            if (task.status == TaskStatus.IN_PROGRESS || task.timerEndAtMillis != null || task.timerRemainingMillis != null) {
                val stopped = task.copy(
                    status = TaskStatus.NOT_STARTED,
                    timerEndAtMillis = null,
                    timerRemainingMillis = null,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(stopped)
                CloudBackupRepository.pushTask(stopped)
            }
        } else {
            dao.deleteRecurringOccurrenceLocally(task, task.seriesId, task.date)
            CloudBackupRepository.deleteTask(task.id)
            CloudBackupRepository.pushRecurringExclusion(task.seriesId, task.date)
        }
    }

    /**
     * The Delete Behavior Contract's "Delete this & future occurrences" choice: [task]'s own date
     * becomes the exact first date its series no longer matches (see
     * [TaskEntity.recurrenceEndEpochDay]) - every past occurrence (any date before [task]'s own)
     * is always left completely untouched, whether [task] is a later occurrence of an already-
     * running series or the series' own definition row itself (in which case there IS no past to
     * preserve, since nothing can be due before a series' own start date - see
     * [TaskEntity.matchesRecurrenceOn] - so this correctly removes the series' entire, otherwise-
     * entirely-future, remaining existence). Returns the series' own updated row (with its new,
     * possibly tighter, end boundary) so the caller can recompute its one reminder against it -
     * null if the series was already removed by a concurrent action.
     */
    suspend fun deleteRecurringThisAndFuture(task: TaskEntity): TaskEntity? {
        val seriesId = task.seriesId ?: task.id
        return capRecurrenceAndSync(seriesId, task.date)
    }

    /**
     * Phase 1 rebuilt deletion for a plain, non-recurring task (repeat NONE, no seriesId) -
     * permanently removes exactly that one task's row, by id only, then mirrors the removal to
     * the cloud backup. Takes only the id (never a TaskEntity snapshot), so nothing here can
     * write any version of the task back. The task's timer state, Pending Task flag
     * (postponeIfIncomplete) and every other field live on that same row, so they go with it.
     * Reminder cancellation and Focus session cleanup are done by the caller - see
     * HomeViewModel.deletePlainTask. Recurring series/occurrence deletion never reaches here:
     * the DAO query itself only matches a non-recurring row.
     */
    suspend fun deletePlainTask(taskId: String) {
        dao.deletePlainTaskById(taskId)
        CloudBackupRepository.deleteTask(taskId)
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
        orderedTasks.forEachIndexed { index, task ->
            val updated = when (scope) {
                TaskOrderScope.ALL -> task.copy(orderInAll = index.toLong(), updatedAt = now)
                TaskOrderScope.IN_PROGRESS -> task.copy(orderInProgress = index.toLong(), updatedAt = now)
                TaskOrderScope.DONE -> task.copy(orderInDone = index.toLong(), updatedAt = now)
            }
            val persisted = dao.upsertUnlessDeletedSince(task.id, task.id in idsWithRealRowAtStart, updated)
            if (persisted) {
                CloudBackupRepository.pushTask(updated)
            }
        }
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
