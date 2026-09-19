package com.nj031.onetask.data.task

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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
     * already has a row, and materializes one for a virtual occurrence on its first use.
     *
     * TEMPORARY DIAGNOSTIC CHANGE (task-deletion-bug runtime investigation): returns the rowId
     * Room already computes internally instead of discarding it, purely so TaskRepository's
     * diagnostic logging can confirm each upsert actually executed - no query or behavior
     * change. Revert to a Unit return once this instrumentation is removed. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    /** TEMPORARY DIAGNOSTIC CHANGE (task-deletion-bug runtime investigation): returns the
     * affected-row count Room already computes internally instead of discarding it, purely so
     * TaskRepository's diagnostic logging can confirm a delete actually removed a row (vs.
     * matching nothing) - no query or behavior change. Revert to a Unit return once this
     * instrumentation is removed. */
    @Delete
    suspend fun delete(task: TaskEntity): Int

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

    // The set of this series' already-materialized-and-completed occurrence dates - used by
    // ReminderScheduler to skip a completed date when computing the next reminder to schedule,
    // without needing every future occurrence to already be a persisted row.
    @Query("SELECT date FROM tasks WHERE seriesId = :seriesId AND status = 'COMPLETED'")
    suspend fun getCompletedOccurrenceDatesForSeries(seriesId: String): List<Long>

    // Cleans up already-materialized occurrences when their series' own definition row is
    // deleted, so deleting a recurring task doesn't leave orphaned individual-date leftovers
    // behind - see TaskRepository.deleteTask.
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
}
