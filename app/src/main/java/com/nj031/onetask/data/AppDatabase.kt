package com.nj031.onetask.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nj031.onetask.data.journal.Converters
import com.nj031.onetask.data.journal.JournalNoteDao
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.task.TaskConverters
import com.nj031.onetask.data.task.TaskDao
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskTagEntity

/** Adds the Notes redesign's noteType/checklistItems columns to the existing journal_notes
 * table in place, so every note a user already saved survives this update - the alternative,
 * relying on fallbackToDestructiveMigration() for this bump like every earlier version bump in
 * this database, would wipe all existing Tasks and Notes data on the next app open. Every
 * existing row gets noteType='TEXT' and an empty checklistItems, which is exactly how
 * Converters.toNoteType/toChecklistItems already interpret those defaults for a plain text note. */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE journal_notes ADD COLUMN noteType TEXT NOT NULL DEFAULT 'TEXT'")
        db.execSQL("ALTER TABLE journal_notes ADD COLUMN checklistItems TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [JournalNoteEntity::class, TaskEntity::class, TaskTagEntity::class],
    version = 6,
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
                ).addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
