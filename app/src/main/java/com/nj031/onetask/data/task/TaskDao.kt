package com.nj031.onetask.data.task

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
     * already has a row, and materializes one for a virtual occurrence on its first use. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

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

    /** Atomically deletes a recurring series' own definition row together with every already-
     * materialized occurrence and per-date exclusion recorded against it - see
     * TaskRepository.deleteTask's own doc comment. Bundling all of it into one Room transaction
     * (rather than the separate, independently-invalidating suspend calls this used to be) means
     * a concurrent observeTasksByDate re-query can never land in between them and see a
     * partially-applied delete (e.g. this series' own row still present, or an occurrence row
     * still present) - Room's Flow observers for getByDate/getActiveRecurringSeries only ever
     * see the fully-applied result or the fully-unapplied one, never a state in between. Returns
     * the occurrence ids that were deleted, so the caller can still mirror each one's removal to
     * the cloud backup afterward (cloud sync itself is untouched by this transaction). */
    @Transaction
    suspend fun deleteRecurringSeriesLocally(seriesTask: TaskEntity): List<String> {
        val occurrenceIds = getOccurrenceIdsForSeries(seriesTask.id)
        deleteOccurrencesForSeries(seriesTask.id)
        deleteRecurringExclusionsForSeries(seriesTask.id)
        delete(seriesTask)
        return occurrenceIds
    }

    /** Atomically records that [epochDay] is excluded going forward AND removes the occurrence's
     * own row (a real row if already materialized, a no-op delete if it was still virtual - see
     * TaskEntity.asVirtualOccurrence) in the SAME Room transaction. Recording the exclusion before
     * deleting the row already closed most of the window where a concurrent observeTasksByDate
     * re-query could see "row gone, not yet excluded" and regenerate the just-deleted occurrence
     * (see this DAO's git history); doing both in one transaction closes it completely, since
     * Room's invalidation can now only ever reflect the fully-applied pair, never one write
     * without the other. */
    @Transaction
    suspend fun deleteRecurringOccurrenceLocally(occurrenceTask: TaskEntity, seriesId: String, epochDay: Long) {
        insertRecurringExclusion(RecurringExclusionEntity(seriesId = seriesId, epochDay = epochDay))
        delete(occurrenceTask)
    }

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
