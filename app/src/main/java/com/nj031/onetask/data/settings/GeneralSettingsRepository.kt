package com.nj031.onetask.data.settings

import android.content.Context
import com.nj031.onetask.data.UserScopedPreferences
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
// Bumped by every setter below except setLastCloudBackupAtMillis, which is device/install
// bookkeeping ("did THIS install last confirm a push"), not an account preference - it's
// deliberately excluded from the cloud-synced snapshot and must never affect this timestamp.
private const val KEY_ACCOUNT_UPDATED_AT = "account_updated_at"

/** Every existing General Settings key that's account-owned (i.e. every one except
 * [GeneralSettingsRepository.getLastCloudBackupAtMillis], which stays local install bookkeeping)
 * - synced as one unit to `users/{uid}/account/generalSettings`, mirroring how
 * [AppearanceSettingsRepository]'s [AppearanceSnapshot] syncs Appearance. [updatedAt] decides
 * which side of a restore wins when both a local and a cloud copy already exist. */
data class GeneralSettingsSnapshot(
    val startScreen: StartScreen,
    val defaultTimerMinutes: Int?,
    val defaultTag: String?,
    val defaultPostponeIfIncomplete: Boolean,
    val focusSessionNotificationsEnabled: Boolean,
    val focusSessionCompleteEnabled: Boolean,
    val weekStartDay: DayOfWeek,
    val timeFormat: TimeFormat,
    val hapticFeedbackEnabled: Boolean,
    val notesViewMode: NotesViewMode,
    val updatedAt: Long
)

/**
 * Stores General Settings in a private SharedPreferences file, the same choice made for
 * UserProfileRepository: AppDatabase has no real Migration objects and falls back to
 * fallbackToDestructiveMigration() on any version bump, so a new Room table for a handful of
 * simple preference values isn't worth risking every existing install's Tasks/Journal data on
 * their next update. The file itself is scoped per signed-in account (see
 * [UserScopedPreferences]) - a plain fixed file name would mean every account on this device
 * shared the exact same settings.
 *
 * This local file remains the fast read/write path every screen already uses; it is no longer
 * the only durable copy - see [getSnapshot]/[applyRemote], used by the account-restore flow.
 */
class GeneralSettingsRepository(context: Context) {
    private val prefs = UserScopedPreferences.open(context, PREFS_NAME)

    private fun touch() = prefs.edit().putLong(KEY_ACCOUNT_UPDATED_AT, System.currentTimeMillis())

    /** Tasks is the default for both existing and new users - anyone who hasn't visited this
     * setting yet keeps launching straight into Tasks, exactly as before this setting existed. */
    fun getStartScreen(): StartScreen {
        val raw = prefs.getString(KEY_START_SCREEN, null) ?: return StartScreen.TASKS
        return runCatching { StartScreen.valueOf(raw) }.getOrDefault(StartScreen.TASKS)
    }

    fun setStartScreen(startScreen: StartScreen) {
        touch().putString(KEY_START_SCREEN, startScreen.name).apply()
    }

    /** null (stored as 0) means "No Timer" - matches the value every new task already started
     * with before this setting existed, so a user who never visits this screen sees no change. */
    fun getDefaultTimerMinutes(): Int? {
        val minutes = prefs.getInt(KEY_DEFAULT_TIMER_MINUTES, 0)
        return if (minutes > 0) minutes else null
    }

    fun setDefaultTimerMinutes(minutes: Int?) {
        touch().putInt(KEY_DEFAULT_TIMER_MINUTES, minutes ?: 0).apply()
    }

    /** null means no default tag - matches the value every new task already started with. */
    fun getDefaultTag(): String? = prefs.getString(KEY_DEFAULT_TAG, null)

    fun setDefaultTag(tag: String?) {
        touch().putString(KEY_DEFAULT_TAG, tag).apply()
    }

    /** true (Pending Task ON) is the default - matches AddTaskScreen's existing hardcoded
     * `existingTask?.postponeIfIncomplete ?: true` for a brand-new task. */
    fun getDefaultPostponeIfIncomplete(): Boolean = prefs.getBoolean(KEY_DEFAULT_POSTPONE_IF_INCOMPLETE, true)

    fun setDefaultPostponeIfIncomplete(postpone: Boolean) {
        touch().putBoolean(KEY_DEFAULT_POSTPONE_IF_INCOMPLETE, postpone).apply()
    }

    /** Both notification toggles default to ON, matching TimerForegroundService's existing
     * always-on behavior before these settings existed. */
    fun getFocusSessionNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_FOCUS_SESSION_NOTIFICATIONS_ENABLED, true)

    fun setFocusSessionNotificationsEnabled(enabled: Boolean) {
        touch().putBoolean(KEY_FOCUS_SESSION_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getFocusSessionCompleteEnabled(): Boolean =
        prefs.getBoolean(KEY_FOCUS_SESSION_COMPLETE_ENABLED, true)

    fun setFocusSessionCompleteEnabled(enabled: Boolean) {
        touch().putBoolean(KEY_FOCUS_SESSION_COMPLETE_ENABLED, enabled).apply()
    }

    /** Monday is the default per spec - this only affects calendar/week presentation
     * (OneTaskCalendarSheet's column order), never task dates or recurring-task logic. */
    fun getWeekStartDay(): DayOfWeek {
        val raw = prefs.getString(KEY_WEEK_START_DAY, null) ?: return DayOfWeek.MONDAY
        return runCatching { DayOfWeek.valueOf(raw) }.getOrDefault(DayOfWeek.MONDAY)
    }

    fun setWeekStartDay(day: DayOfWeek) {
        touch().putString(KEY_WEEK_START_DAY, day.name).apply()
    }

    fun getTimeFormat(): TimeFormat {
        val raw = prefs.getString(KEY_TIME_FORMAT, null) ?: return TimeFormat.SYSTEM_DEFAULT
        return runCatching { TimeFormat.valueOf(raw) }.getOrDefault(TimeFormat.SYSTEM_DEFAULT)
    }

    fun setTimeFormat(timeFormat: TimeFormat) {
        touch().putString(KEY_TIME_FORMAT, timeFormat.name).apply()
    }

    /** ON is the default per spec. */
    fun getHapticFeedbackEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, true)

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        touch().putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, enabled).apply()
    }

    /** List View is the default the first time the Notes screen is opened; once the user
     * switches, that choice persists across app restarts instead of resetting. */
    fun getNotesViewMode(): NotesViewMode {
        val raw = prefs.getString(KEY_NOTES_VIEW_MODE, null) ?: return NotesViewMode.LIST
        return runCatching { NotesViewMode.valueOf(raw) }.getOrDefault(NotesViewMode.LIST)
    }

    fun setNotesViewMode(mode: NotesViewMode) {
        touch().putString(KEY_NOTES_VIEW_MODE, mode.name).apply()
    }

    /** null means a cloud "Backup Now" has never completed on this device. Deliberately local-
     * only bookkeeping (see [KEY_ACCOUNT_UPDATED_AT]'s own comment) - never included in
     * [getSnapshot]/[applyRemote]. */
    fun getLastCloudBackupAtMillis(): Long? {
        val millis = prefs.getLong(KEY_LAST_CLOUD_BACKUP_AT, 0L)
        return if (millis > 0) millis else null
    }

    fun setLastCloudBackupAtMillis(millis: Long) {
        prefs.edit().putLong(KEY_LAST_CLOUD_BACKUP_AT, millis).apply()
    }

    fun getAccountUpdatedAt(): Long = prefs.getLong(KEY_ACCOUNT_UPDATED_AT, 0L)

    fun getSnapshot(): GeneralSettingsSnapshot = GeneralSettingsSnapshot(
        startScreen = getStartScreen(),
        defaultTimerMinutes = getDefaultTimerMinutes(),
        defaultTag = getDefaultTag(),
        defaultPostponeIfIncomplete = getDefaultPostponeIfIncomplete(),
        focusSessionNotificationsEnabled = getFocusSessionNotificationsEnabled(),
        focusSessionCompleteEnabled = getFocusSessionCompleteEnabled(),
        weekStartDay = getWeekStartDay(),
        timeFormat = getTimeFormat(),
        hapticFeedbackEnabled = getHapticFeedbackEnabled(),
        notesViewMode = getNotesViewMode(),
        updatedAt = getAccountUpdatedAt()
    )

    /** Applies a cloud-restored General Settings snapshot locally - used only by the post-sign-in
     * restore, never by any settings screen's own toggles/pickers (which go through the
     * individual setters above instead). Writes [snapshot]'s own updatedAt (the cloud value being
     * restored), not "now", so this restore is never mistaken for a newer local edit on the very
     * next sync. */
    fun applyRemote(snapshot: GeneralSettingsSnapshot) {
        prefs.edit()
            .putString(KEY_START_SCREEN, snapshot.startScreen.name)
            .putInt(KEY_DEFAULT_TIMER_MINUTES, snapshot.defaultTimerMinutes ?: 0)
            .putString(KEY_DEFAULT_TAG, snapshot.defaultTag)
            .putBoolean(KEY_DEFAULT_POSTPONE_IF_INCOMPLETE, snapshot.defaultPostponeIfIncomplete)
            .putBoolean(KEY_FOCUS_SESSION_NOTIFICATIONS_ENABLED, snapshot.focusSessionNotificationsEnabled)
            .putBoolean(KEY_FOCUS_SESSION_COMPLETE_ENABLED, snapshot.focusSessionCompleteEnabled)
            .putString(KEY_WEEK_START_DAY, snapshot.weekStartDay.name)
            .putString(KEY_TIME_FORMAT, snapshot.timeFormat.name)
            .putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, snapshot.hapticFeedbackEnabled)
            .putString(KEY_NOTES_VIEW_MODE, snapshot.notesViewMode.name)
            .putLong(KEY_ACCOUNT_UPDATED_AT, snapshot.updatedAt)
            .apply()
    }
}
