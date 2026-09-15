package com.nj031.onetask.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nj031.onetask.data.journal.Converters
import com.nj031.onetask.data.journal.JournalNoteDao
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.task.TaskConverters
import com.nj031.onetask.data.task.TaskDao
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskTagEntity

@Database(
    entities = [JournalNoteEntity::class, TaskEntity::class, TaskTagEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class, TaskConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalNoteDao(): JournalNoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "one_task_database"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
