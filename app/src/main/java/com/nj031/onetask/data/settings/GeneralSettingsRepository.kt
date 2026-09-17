package com.nj031.onetask.data.settings

import android.content.Context
import java.time.DayOfWeek

enum class StartScreen { TASKS, JOURNAL, TIMER }

enum class TimeFormat { SYSTEM_DEFAULT, HOUR_12, HOUR_24 }

enum class NotesViewMode { LIST, CARD }

private const val PREFS_NAME = "general_settings_prefs"
private const val KEY_START_SCREEN = "start_screen"
private const val KEY_DEFAULT_TIMER_MINUTES = "default_timer_minutes"
private const val KEY_DEFAULT_TAG = "default_tag"
private const val KEY_DEFAULT_POSTPONE_IF_INCOMPLETE = "default_postpone_if_incomplete"
private const val KEY_FOCUS_SESSION_NOTIFICATIONS_ENABLED = "focus_session_notifications_enabled"
private const val KEY_FOCUS_SESSION_COMPLETE_ENABLED = "focus_session_complete_enabled"
private const val KEY_WEEK_START_DAY = "week_start_day"
private const val KEY_TIME_FORMAT = "time_format"
private const val KEY_HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
private const val KEY_NOTES_VIEW_MODE = "notes_view_mode"
private const val KEY_LAST_CLOUD_BACKUP_AT = "last_cloud_backup_at"

/**
 * Stores General Settings in a private SharedPreferences file, the same choice made for
 * UserProfileRepository: AppDatabase has no real Migration objects and falls back to
 * fallbackToDestructiveMigration() on any version bump, so a new Room table for a handful of
 * simple preference values isn't worth risking every existing install's Tasks/Journal data on
 * their next update.
 */
class GeneralSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Tasks is the default for both existing and new users - anyone who hasn't visited this
     * setting yet keeps launching straight into Tasks, exactly as before this setting existed. */
    fun getStartScreen(): StartScreen {
        val raw = prefs.getString(KEY_START_SCREEN, null) ?: return StartScreen.TASKS
        return runCatching { StartScreen.valueOf(raw) }.getOrDefault(StartScreen.TASKS)
    }

    fun setStartScreen(startScreen: StartScreen) {
        prefs.edit().putString(KEY_START_SCREEN, startScreen.name).apply()
    }

    /** null (stored as 0) means "No Timer" - matches the value every new task already started
     * with before this setting existed, so a user who never visits this screen sees no change. */
    fun getDefaultTimerMinutes(): Int? {
        val minutes = prefs.getInt(KEY_DEFAULT_TIMER_MINUTES, 0)
        return if (minutes > 0) minutes else null
    }

    fun setDefaultTimerMinutes(minutes: Int?) {
        prefs.edit().putInt(KEY_DEFAULT_TIMER_MINUTES, minutes ?: 0).apply()
    }

    /** null means no default tag - matches the value every new task already started with. */
    fun getDefaultTag(): String? = prefs.getString(KEY_DEFAULT_TAG, null)

    fun setDefaultTag(tag: String?) {
        prefs.edit().putString(KEY_DEFAULT_TAG, tag).apply()
    }

    /** true (Pending Task ON) is the default - matches AddTaskScreen's existing hardcoded
     * `existingTask?.postponeIfIncomplete ?: true` for a brand-new task. */
    fun getDefaultPostponeIfIncomplete(): Boolean = prefs.getBoolean(KEY_DEFAULT_POSTPONE_IF_INCOMPLETE, true)

    fun setDefaultPostponeIfIncomplete(postpone: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_POSTPONE_IF_INCOMPLETE, postpone).apply()
    }

    /** Both notification toggles default to ON, matching TimerForegroundService's existing
     * always-on behavior before these settings existed. */
    fun getFocusSessionNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_FOCUS_SESSION_NOTIFICATIONS_ENABLED, true)

    fun setFocusSessionNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_SESSION_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getFocusSessionCompleteEnabled(): Boolean =
        prefs.getBoolean(KEY_FOCUS_SESSION_COMPLETE_ENABLED, true)

    fun setFocusSessionCompleteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_SESSION_COMPLETE_ENABLED, enabled).apply()
    }

    /** Monday is the default per spec - this only affects calendar/week presentation
     * (OneTaskCalendarSheet's column order), never task dates or recurring-task logic. */
    fun getWeekStartDay(): DayOfWeek {
        val raw = prefs.getString(KEY_WEEK_START_DAY, null) ?: return DayOfWeek.MONDAY
        return runCatching { DayOfWeek.valueOf(raw) }.getOrDefault(DayOfWeek.MONDAY)
    }

    fun setWeekStartDay(day: DayOfWeek) {
        prefs.edit().putString(KEY_WEEK_START_DAY, day.name).apply()
    }

    fun getTimeFormat(): TimeFormat {
        val raw = prefs.getString(KEY_TIME_FORMAT, null) ?: return TimeFormat.SYSTEM_DEFAULT
        return runCatching { TimeFormat.valueOf(raw) }.getOrDefault(TimeFormat.SYSTEM_DEFAULT)
    }

    fun setTimeFormat(timeFormat: TimeFormat) {
        prefs.edit().putString(KEY_TIME_FORMAT, timeFormat.name).apply()
    }

    /** ON is the default per spec. */
    fun getHapticFeedbackEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, true)

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, enabled).apply()
    }

    /** List View is the default the first time the Notes screen is opened; once the user
     * switches, that choice persists across app restarts instead of resetting. */
    fun getNotesViewMode(): NotesViewMode {
        val raw = prefs.getString(KEY_NOTES_VIEW_MODE, null) ?: return NotesViewMode.LIST
        return runCatching { NotesViewMode.valueOf(raw) }.getOrDefault(NotesViewMode.LIST)
    }

    fun setNotesViewMode(mode: NotesViewMode) {
        prefs.edit().putString(KEY_NOTES_VIEW_MODE, mode.name).apply()
    }

    /** null means a cloud "Backup Now" has never completed on this device. */
    fun getLastCloudBackupAtMillis(): Long? {
        val millis = prefs.getLong(KEY_LAST_CLOUD_BACKUP_AT, 0L)
        return if (millis > 0) millis else null
    }

    fun setLastCloudBackupAtMillis(millis: Long) {
        prefs.edit().putLong(KEY_LAST_CLOUD_BACKUP_AT, millis).apply()
    }
}
