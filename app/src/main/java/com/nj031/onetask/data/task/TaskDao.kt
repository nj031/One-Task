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

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY createdAt ASC")
    fun getByDate(date: Long): Flow<List<TaskEntity>>

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
