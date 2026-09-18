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
     * already has a row, and materializes one for a virtual occurrence on its first use. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY createdAt ASC")
    fun getByDate(date: Long): Flow<List<TaskEntity>>

    // A recurring series' own definition row (never a materialized occurrence - see
    // TaskEntity.seriesId) - combined in TaskRepository with getByDate/getDatesWithTasksBetween
    // to compute which other dates a series is also due on, without persisting a row for every
    // future occurrence up front.
    @Query("SELECT * FROM tasks WHERE repeat != 'NONE' AND seriesId IS NULL")
    fun getActiveRecurringSeries(): Flow<List<TaskEntity>>

    @Query("SELECT id FROM tasks WHERE seriesId = :seriesId")
    suspend fun getOccurrenceIdsForSeries(seriesId: String): List<String>

    // Cleans up already-materialized occurrences when their series' own definition row is
    // deleted, so deleting a recurring task doesn't leave orphaned individual-date leftovers
    // behind - see TaskRepository.deleteTask.
    @Query("DELETE FROM tasks WHERE seriesId = :seriesId")
    suspend fun deleteOccurrencesForSeries(seriesId: String)

    // Backs the calendar's per-date task indicator dot - only which dates have at least one
    // task, not the tasks themselves, for whatever month range the calendar currently has open.
    @Query("SELECT DISTINCT date FROM tasks WHERE date BETWEEN :startDate AND :endDate")
    fun getDatesWithTasksBetween(startDate: Long, endDate: Long): Flow<List<Long>>

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
}
