package com.nj031.onetask.data.task

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** [TaskDao.capRecurrenceAt]'s result: the series row's own state after its end boundary was
 * applied, and the ids of every already-materialized occurrence row this pruned (now at or past
 * that boundary, so it can never be regenerated). */
data class RecurrenceCapResult(val updatedSeries: TaskEntity, val prunedOccurrenceIds: List<String>)

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    /** Same insert-or-replace-by-id behavior as [insert] - used at every "persist a change to a
     * task that's expected to already exist" call site instead of [update], since a virtual
     * (not-yet-persisted) recurring occurrence - see [TaskEntity.asVirtualOccurrence] - has no
     * existing row for a plain @Update to match. Behaves exactly like [update] for a task that
     * already has a row, and materializes one for a virtual occurrence on its first use. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    /** Phase 1 rebuilt plain-task delete - see TaskRepository.deletePlainTask. Deletes by id
     * alone (never from a possibly-stale entity snapshot), and the seriesId/repeat guard means it
     * can only ever match a non-recurring task's own row - never a recurring series definition
     * or a materialized occurrence, whose deletion stays on the existing recurring paths above.
     * Returns the number of rows deleted (0 or 1). */
    @Query("DELETE FROM tasks WHERE id = :taskId AND seriesId IS NULL AND repeat = 'NONE'")
    suspend fun deletePlainTaskById(taskId: String): Int

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY createdAt ASC")
    fun getByDate(date: Long): Flow<List<TaskEntity>>

    // A recurring series' own definition row (never a materialized occurrence - see
    // TaskEntity.seriesId) - combined in TaskRepository with getByDate/getTasksWithDateBetween
    // to compute which other dates a series is also due on, without persisting a row for every
    // future occurrence up front.
    @Query("SELECT * FROM tasks WHERE repeat != 'NONE' AND seriesId IS NULL")
    fun getActiveRecurringSeries(): Flow<List<TaskEntity>>

    @Query("SELECT id FROM tasks WHERE seriesId = :seriesId")
    suspend fun getOccurrenceIdsForSeries(seriesId: String): List<String>

    /** Atomically records that [epochDay] is excluded going forward AND removes the occurrence's
     * own row (a real row if already materialized, a no-op delete if it was still virtual - see
     * TaskEntity.asVirtualOccurrence) in the SAME Room transaction. Recording the exclusion before
     * deleting the row already closed most of the window where a concurrent observeTasksByDate
     * re-query could see "row gone, not yet excluded" and regenerate the just-deleted occurrence
     * (see this DAO's git history); doing both in one transaction closes it completely, since
     * Room's invalidation can now only ever reflect the fully-applied pair, never one write
     * without the other. Used only for "this occurrence" on a date OTHER than the series' own
     * start date - see [excludeSeriesOwnStartDateLocally] for that one, and
     * TaskRepository.deleteRecurringOccurrence for which of the two a given delete uses. */
    @Transaction
    suspend fun deleteRecurringOccurrenceLocally(occurrenceTask: TaskEntity, seriesId: String, epochDay: Long) {
        insertRecurringExclusion(RecurringExclusionEntity(seriesId = seriesId, epochDay = epochDay))
        delete(occurrenceTask)
    }

    /** "This occurrence" when the selected occurrence IS the series' own start date - i.e. the
     * literal row being acted on is the series definition row itself ([TaskEntity.seriesId] ==
     * null), not a separate materialized/virtual occurrence row. That row must never be deleted
     * here (deleting it would remove the series' pattern entirely, taking every later date's
     * occurrence down with it - exactly the forbidden "delete entire series including past"
     * behavior); the Delete Behavior Contract's "this occurrence" only ever removes the one
     * selected date, so this records an exclusion for the series' own start date and leaves the
     * row itself untouched. [observeTasksByDate] already checks this same exclusion set before
     * showing the series row on its own start date - see its own doc comment. */
    suspend fun excludeSeriesOwnStartDateLocally(seriesId: String, epochDay: Long) {
        insertRecurringExclusion(RecurringExclusionEntity(seriesId = seriesId, epochDay = epochDay))
    }

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getByIdOnce(id: String): TaskEntity?

    @Query("SELECT id FROM tasks WHERE seriesId = :seriesId AND date >= :fromEpochDay")
    suspend fun getOccurrenceIdsForSeriesFromDate(seriesId: String, fromEpochDay: Long): List<String>

    @Query("DELETE FROM tasks WHERE seriesId = :seriesId AND date >= :fromEpochDay")
    suspend fun deleteOccurrencesForSeriesFromDate(seriesId: String, fromEpochDay: Long)

    @Query("DELETE FROM recurring_exclusions WHERE seriesId = :seriesId AND epochDay >= :fromEpochDay")
    suspend fun deleteRecurringExclusionsForSeriesFromDate(seriesId: String, fromEpochDay: Long)

    /** Applies (and, if a tighter one already exists, never loosens) an end boundary on
     * [seriesId] at [cutoffEpochDay] - see [TaskEntity.recurrenceEndEpochDay] - the shared
     * primitive behind both the Delete Behavior Contract's "this & future" choice
     * (TaskRepository.deleteRecurringThisAndFuture) and detaching an edited occurrence from its
     * old series when its own date/repeat changes (TaskRepository.updateTask). Also prunes every
     * already-materialized occurrence row of this series at or after the cutoff - once excluded
     * from the pattern they can never be regenerated, so leaving them behind would be permanently
     * orphaned rows - and every now-redundant per-date exclusion at or after the cutoff, since
     * [TaskEntity.matchesRecurrenceOn] already covers every one of those dates going forward on
     * its own.
     *
     * If the series' own start date falls at or after the resulting boundary, the series row
     * itself is now excluded from its own pattern too (this is the "this & future on the very
     * first occurrence" case - equivalent to removing the series' entire, otherwise-entirely-
     * future, existence) - if that row still carries a live timer (see TIMER + DELETE in the
     * Delete Behavior Contract), it's stopped in the same write, exactly like [resetTimer] does:
     * every future occurrence already starts fresh regardless of this row's own runtime state
     * (see [TaskEntity.asVirtualOccurrence]), so leaving it running would only ever mean an
     * orphaned foreground timer for a date that can never be reached again.
     *
     * Wrapped in one transaction so a concurrent observeTasksByDate re-query can never see the
     * boundary applied without the now-invalid occurrences/exclusions already pruned, or vice
     * versa. Returns the pruned occurrence ids (for the caller to mirror to the cloud backup) and
     * the series row's own resulting state (for the caller to recompute its one reminder against
     * the new, possibly now-empty, matching range) - null if the series row no longer exists (e.g.
     * already deleted by a concurrent action). */
    @Transaction
    suspend fun capRecurrenceAt(seriesId: String, cutoffEpochDay: Long): RecurrenceCapResult? {
        val series = getByIdOnce(seriesId) ?: return null
        val existingEnd = series.recurrenceEndEpochDay
        val newEnd = if (existingEnd == null) cutoffEpochDay else minOf(existingEnd, cutoffEpochDay)
        val prunedIds = getOccurrenceIdsForSeriesFromDate(seriesId, newEnd)
        deleteOccurrencesForSeriesFromDate(seriesId, newEnd)
        deleteRecurringExclusionsForSeriesFromDate(seriesId, newEnd)
        val ownDateNowExcluded = series.date >= newEnd
        val hasLiveTimer = series.status == TaskStatus.IN_PROGRESS ||
            series.timerEndAtMillis != null ||
            series.timerRemainingMillis != null
        val stopTimer = ownDateNowExcluded && hasLiveTimer
        val updatedSeries = series.copy(
            recurrenceEndEpochDay = newEnd,
            status = if (stopTimer) TaskStatus.NOT_STARTED else series.status,
            timerEndAtMillis = if (stopTimer) null else series.timerEndAtMillis,
            timerRemainingMillis = if (stopTimer) null else series.timerRemainingMillis
        )
        update(updatedSeries)
        return RecurrenceCapResult(updatedSeries, prunedIds)
    }

    // The set of this series' already-materialized-and-completed occurrence dates - used by
    // ReminderScheduler to skip a completed date when computing the next reminder to schedule,
    // without needing every future occurrence to already be a persisted row.
    @Query("SELECT date FROM tasks WHERE seriesId = :seriesId AND status = 'COMPLETED'")
    suspend fun getCompletedOccurrenceDatesForSeries(seriesId: String): List<Long>

    // Deletes every already-materialized occurrence of a series in one go - currently unused by
    // any delete path (the Delete Behavior Contract has no "delete entire series including past"
    // operation; see capRecurrenceAt/deleteOccurrencesForSeriesFromDate for the bounded version
    // "this & future" actually uses), kept only as a general-purpose primitive.
    @Query("DELETE FROM tasks WHERE seriesId = :seriesId")
    suspend fun deleteOccurrencesForSeries(seriesId: String)

    // Backs the calendar's per-date task indicator dot, for whatever month range the calendar
    // currently has open. Returns full rows (not just distinct dates) so TaskRepository can
    // apply TaskEntity.isDueOn - a plain SELECT DISTINCT date can't tell a recurring series' own
    // start-date row, which might not itself satisfy the series' pattern, from a task that's
    // genuinely due on that date.
    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate")
    fun getTasksWithDateBetween(startDate: Long, endDate: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    // Which of [ids] currently have a real row - used only by upsertUnlessDeletedSince below to
    // tell "this task was deleted by something else since we last looked" apart from "this task
    // never had a row yet" (a still-virtual recurring occurrence - see
    // TaskEntity.asVirtualOccurrence), which must keep materializing normally.
    @Query("SELECT id FROM tasks WHERE id IN (:ids)")
    suspend fun getExistingIds(ids: List<String>): List<String>

    /** Persists [updated] unless [hadRealRowAtStart] is true and its row has since been deleted
     * by some other, concurrent write - see TaskRepository.reorderTasks's own doc comment for the
     * race this closes. Wrapped in one transaction so the existence check and the upsert can
     * never be split by a concurrent delete landing in between them. A task that never had a row
     * to begin with ([hadRealRowAtStart] false - a still-virtual recurring occurrence) is
     * unaffected: it always persists exactly as it already did before this method existed.
     * Returns whether the row was actually written, so the caller can skip mirroring a skipped
     * write to the cloud backup too. */
    @Transaction
    suspend fun upsertUnlessDeletedSince(taskId: String, hadRealRowAtStart: Boolean, updated: TaskEntity): Boolean {
        if (hadRealRowAtStart && getExistingIds(listOf(taskId)).isEmpty()) return false
        upsert(updated)
        return true
    }

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getById(id: String): Flow<TaskEntity?>

    @Query("UPDATE tasks SET date = :today, updatedAt = :now WHERE postponeIfIncomplete = 1 AND status = 'NOT_STARTED' AND date < :today")
    suspend fun postponeOverdueTasks(today: Long, now: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TaskTagEntity)

    @Query("SELECT name FROM task_tags ORDER BY name ASC")
    fun getCustomTags(): Flow<List<String>>

    @Query("SELECT name FROM task_tags ORDER BY name ASC")
    suspend fun getCustomTagsOnce(): List<String>

    // Only removes the tag from the available-tags list (task_tags) - never touches the tasks
    // table, so any task that already used this tag keeps its tag string exactly as before.
    @Query("DELETE FROM task_tags WHERE name = :name")
    suspend fun deleteTag(name: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM task_tags")
    suspend fun deleteAllTags()

    // See RecurringExclusionEntity's own doc comment - this is what makes deleting a single
    // occurrence of an active recurring series actually stick, rather than being silently
    // regenerated by TaskRepository.observeTasksByDate on its very next emission.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecurringExclusion(exclusion: RecurringExclusionEntity)

    @Query("SELECT seriesId FROM recurring_exclusions WHERE epochDay = :date")
    fun getExcludedSeriesIdsForDate(date: Long): Flow<List<String>>

    @Query("SELECT * FROM recurring_exclusions")
    suspend fun getAllRecurringExclusions(): List<RecurringExclusionEntity>

    // Cleans up exclusions when their series' own definition row is deleted entirely, mirroring
    // deleteOccurrencesForSeries above - a deleted series shouldn't leave orphaned exclusion
    // rows behind either.
    @Query("DELETE FROM recurring_exclusions WHERE seriesId = :seriesId")
    suspend fun deleteRecurringExclusionsForSeries(seriesId: String)

    // REPLACE (not IGNORE, unlike insertTag) - a category's id is stable but its name can change
    // (see TaskRepository.renameCustomCategory), so re-inserting the same id must overwrite the
    // existing row rather than silently no-op.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    fun getCustomCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    suspend fun getCustomCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun renameCategory(id: String, name: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    // Every task currently assigned the category about to be deleted - fetched BEFORE
    // clearCategoryFromTasks below so TaskRepository.deleteCustomCategory can mirror the same
    // "become No Category" change to each one's cloud copy too.
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId")
    suspend fun getTasksByCategoryId(categoryId: String): List<TaskEntity>

    // Deleting a category must cascade to every task that had it (unlike deleteTag, which
    // deliberately never touches the tasks table) - every affected task becomes "No Category"
    // rather than being left referencing a category id that no longer exists, per the Category
    // spec's delete-confirmation behavior.
    @Query("UPDATE tasks SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromTasks(categoryId: String)
}
