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
import com.nj031.onetask.data.journal.NoteLabelEntity
import com.nj031.onetask.data.task.RecurringExclusionEntity
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

/** Adds the Notes Labels feature's columns/table in place, so every existing note and its data
 * survive this update: journal_notes gets a nullable label column (absent = no label, exactly
 * how every note already saved reads once this migrates), and note_labels is a brand-new,
 * initially-empty table for the set of labels available to assign. */
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE journal_notes ADD COLUMN label TEXT")
        db.execSQL("CREATE TABLE IF NOT EXISTS note_labels (name TEXT NOT NULL PRIMARY KEY)")
    }
}

/** Adds the recurring-tasks fix's columns to the existing tasks table in place, so every
 * existing task survives this update: repeatDays defaults to empty (unused until a task's
 * repeat is actually SELECT_DAYS) and seriesId defaults to null (every existing row is a
 * plain/one-time task, never a materialized recurring occurrence - that concept didn't exist
 * before this update). Also drops the Repeat picker's old WEEKLY/MONTHLY options, which never
 * actually affected which dates a task showed on (repeat was purely a label) - converting any
 * existing WEEKLY/MONTHLY task to NONE here doesn't change its observable behavior at all (it
 * already only ever showed on its own literal date), it just retires those two enum values
 * safely before Room would otherwise fail to read them back. */
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN repeatDays TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN seriesId TEXT")
        db.execSQL("UPDATE tasks SET repeat = 'NONE' WHERE repeat IN ('WEEKLY', 'MONTHLY')")
    }
}

/** Adds the Priority feature's column to the existing tasks table in place: every task that
 * existed before this update gets 'NONE' (no priority selected), exactly the same default a
 * brand-new task without a chosen priority gets, rather than guessing Small/Medium/High for
 * them. */
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'NONE'")
    }
}

/** Adds the Reminder feature's columns to the existing tasks table in place: both are nullable
 * with no explicit default, so every task that existed before this update gets NULL for both -
 * exactly "no reminder configured", the same state a brand-new task without a reminder gets. */
private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderMinuteOfDay INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderEpochDay INTEGER")
    }
}

/** Adds the Success Condition feature's columns to the existing tasks table in place: every task
 * that existed before this update gets successCondition = 'ALL' (the same default a brand-new
 * task without an explicit choice gets) and a null successConditionThreshold (only meaningful for
 * CUSTOM) - together, the same "every subtask must be completed" behavior an unset condition
 * already implies for a task with subtasks, and a no-op for a task without any. */
private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN successCondition TEXT NOT NULL DEFAULT 'ALL'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN successConditionThreshold INTEGER")
    }
}

/** Adds the recurring-occurrence-deletion fix's table: a brand-new, initially-empty table, so
 * every existing task/note survives this update untouched. Fixes the bug where deleting a single
 * occurrence of an active recurring series (Daily/Select Days) - materialized or not - silently
 * reappeared, because nothing recorded that the user had explicitly deleted that (series, date)
 * pair; see RecurringExclusionEntity and TaskRepository.deleteTask/observeTasksByDate. */
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS recurring_exclusions (" +
                "seriesId TEXT NOT NULL, epochDay INTEGER NOT NULL, " +
                "PRIMARY KEY(seriesId, epochDay))"
        )
    }
}

@Database(
    entities = [
        JournalNoteEntity::class,
        NoteLabelEntity::class,
        TaskEntity::class,
        TaskTagEntity::class,
        RecurringExclusionEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class, TaskConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalNoteDao(): JournalNoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        private const val LEGACY_DATABASE_NAME = "one_task_database"

        @Volatile
        private var instance: AppDatabase? = null
        @Volatile
        private var instanceUserId: String? = null

        /**
         * Returns the Room database for whichever account is CURRENTLY signed in (see
         * [UserScope]) - never the single database every account on this device used to share
         * before this existed. Exactly one connection is ever held open: if the signed-in
         * account has changed since the cached instance was opened, that connection is closed
         * and a fresh one is opened for the new account, so a stale connection to the previous
         * account's file can never linger and two accounts' connections are never open at once.
         *
         * Deliberately does NOT claim/copy anything from the legacy, pre-per-account database
         * ([LEGACY_DATABASE_NAME], still referenced here only as the name every install prior to
         * per-account storage used): there is no reliable way to know which account its rows
         * actually belonged to, and assigning them to whichever account happens to open the app
         * first is an unverified guess, not a real ownership determination. That file is simply
         * left on disk, untouched and unread, forever; every account (including the very first
         * to sign in after per-account storage was introduced) starts from a genuinely empty
         * database instead.
         */
        fun getInstance(context: Context): AppDatabase {
            val appContext = context.applicationContext
            val userId = UserScope.id()
            instance?.let { if (instanceUserId == userId) return it }
            synchronized(this) {
                instance?.let { if (instanceUserId == userId) return it }
                instance?.close()
                val databaseName = "${LEGACY_DATABASE_NAME}_$userId"
                val database = Room.databaseBuilder(appContext, AppDatabase::class.java, databaseName)
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .fallbackToDestructiveMigration()
                    .build()
                instance = database
                instanceUserId = userId
                return database
            }
        }
    }
}
