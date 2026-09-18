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
import com.nj031.onetask.data.task.TaskConverters
import com.nj031.onetask.data.task.TaskDao
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskTagEntity
import java.io.File

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

@Database(
    entities = [JournalNoteEntity::class, NoteLabelEntity::class, TaskEntity::class, TaskTagEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class, TaskConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalNoteDao(): JournalNoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        private const val LEGACY_DATABASE_NAME = "one_task_database"
        private const val MIGRATION_FLAGS_PREFS = "storage_migration_flags"
        private const val KEY_LEGACY_DATABASE_CLAIMED = "legacy_database_claimed"

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
         */
        fun getInstance(context: Context): AppDatabase {
            val appContext = context.applicationContext
            val userId = UserScope.id()
            instance?.let { if (instanceUserId == userId) return it }
            synchronized(this) {
                instance?.let { if (instanceUserId == userId) return it }
                instance?.close()
                val databaseName = "${LEGACY_DATABASE_NAME}_$userId"
                claimLegacyDatabaseIfNeeded(appContext, databaseName)
                val database = Room.databaseBuilder(appContext, AppDatabase::class.java, databaseName)
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                instance = database
                instanceUserId = userId
                return database
            }
        }

        /**
         * One-time, non-destructive claim: the very first account to open the app after
         * per-account databases were introduced inherits whatever was in the single database
         * every account previously shared - nothing is deleted, the legacy file is simply left
         * in place (untouched, no longer read from) once this runs. Every OTHER account that
         * signs in after that on this device starts from an empty database instead of also
         * inheriting it, since there is no way to know which of the old database's rows were
         * really theirs - that was never recorded - and guessing risks a worse privacy bug than
         * an empty first launch for those accounts.
         */
        private fun claimLegacyDatabaseIfNeeded(context: Context, newDatabaseName: String) {
            val flags = context.getSharedPreferences(MIGRATION_FLAGS_PREFS, Context.MODE_PRIVATE)
            if (flags.getBoolean(KEY_LEGACY_DATABASE_CLAIMED, false)) return
            val legacyFile = context.getDatabasePath(LEGACY_DATABASE_NAME)
            if (legacyFile.exists()) {
                val newFile = context.getDatabasePath(newDatabaseName)
                if (!newFile.exists()) {
                    runCatching { legacyFile.copyTo(newFile, overwrite = false) }
                    // Room's default WAL journal mode can leave -wal/-shm companion files
                    // holding not-yet-checkpointed writes - copy those too, if present, so
                    // nothing committed to disk is silently dropped by copying the main file
                    // alone.
                    for (suffix in listOf("-wal", "-shm")) {
                        val source = File(legacyFile.path + suffix)
                        if (source.exists()) {
                            runCatching { source.copyTo(File(newFile.path + suffix), overwrite = false) }
                        }
                    }
                }
            }
            flags.edit().putBoolean(KEY_LEGACY_DATABASE_CLAIMED, true).apply()
        }
    }
}
