package com.nj031.onetask.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.backup.DataExportFormat
import com.nj031.onetask.data.journal.JournalRepository
import com.nj031.onetask.data.profile.UserProfileRepository
import com.nj031.onetask.data.settings.AppearanceSettingsRepository
import com.nj031.onetask.data.settings.GeneralSettingsRepository
import com.nj031.onetask.data.sync.CloudAppearance
import com.nj031.onetask.data.sync.CloudBackupRepository
import com.nj031.onetask.data.sync.CloudProfile
import com.nj031.onetask.data.task.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Backs the Data & Privacy screen: export/restore/wipe operate on the same TaskRepository/
 * JournalRepository every other screen uses, so a merge (restore) or wipe (delete-all) here is
 * immediately reflected everywhere else in the app without any extra plumbing.
 */
class DataPrivacyViewModel(application: Application) : AndroidViewModel(application) {
    private val taskRepository = TaskRepository(AppDatabase.getInstance(application).taskDao())
    private val journalRepository = JournalRepository(AppDatabase.getInstance(application).journalNoteDao())
    private val settingsRepository = GeneralSettingsRepository(application)
    private val appearanceRepository = AppearanceSettingsRepository(application)
    private val profileRepository = UserProfileRepository(application)

    private val _lastBackupAtMillis = MutableStateFlow(settingsRepository.getLastCloudBackupAtMillis())
    val lastBackupAtMillis: StateFlow<Long?> = _lastBackupAtMillis.asStateFlow()

    /** Pushes every account-owned item up to Cloud Firestore/Storage right now, on top of the
     * app's existing always-on per-write mirroring - useful right after a bulk restore, or
     * simply to confirm everything's up to date - then records when it finished. Every category
     * introduced for account persistence (Labels, Appearance, General Settings, Profile) is
     * included here alongside the original Tasks/Notes/Tags, so this button's own "confirm
     * everything's up to date" promise stays accurate as new account-owned data is added -
     * deliberately excludes the profile photo, which is only ever pushed when it actually
     * changes (see ProfileViewModel), since re-uploading an unchanged image on every "Backup
     * Now" tap would be pure waste. */
    suspend fun backupNow() {
        val tasks = taskRepository.getAllTasksOnce()
        val notes = journalRepository.getAllNotesOnce()
        val tags = taskRepository.getAllCustomTagsOnce()
        val labels = journalRepository.getAllLabelsOnce()
        CloudBackupRepository.pushAllTasks(tasks)
        CloudBackupRepository.pushAllNotes(notes)
        CloudBackupRepository.pushAllTags(tags)
        CloudBackupRepository.pushAllLabels(labels)

        val appearance = appearanceRepository.getSnapshot()
        CloudBackupRepository.pushAppearance(
            CloudAppearance(appearance.displayMode.name, appearance.colorTheme.name, appearance.updatedAt)
        )
        CloudBackupRepository.pushGeneralSettings(settingsRepository.getSnapshot().toCloud())
        val profile = profileRepository.getProfile(defaultName = AuthRepository.currentUser?.displayName.orEmpty())
        CloudBackupRepository.pushProfile(
            CloudProfile(profile.name, profile.dateOfBirth, profile.gender?.name, profileRepository.getUpdatedAt())
        )

        val now = System.currentTimeMillis()
        settingsRepository.setLastCloudBackupAtMillis(now)
        _lastBackupAtMillis.value = now
    }

    suspend fun exportDataJson(): String {
        val tasks = taskRepository.getAllTasksOnce()
        val notes = journalRepository.getAllNotesOnce()
        val tags = taskRepository.getAllCustomTagsOnce()
        return DataExportFormat.toJson(tasks, notes, tags)
    }

    /** Merges a previously exported backup into the current account: matching ids are
     * updated, new ids are added, and nothing already present but absent from the file is
     * touched or removed. */
    suspend fun restoreDataJson(json: String) {
        val parsed = DataExportFormat.parse(json)
        taskRepository.restoreTasks(parsed.tasks, parsed.tags)
        journalRepository.restoreNotes(parsed.notes)
    }

    /** Permanently deletes all Task/Journal data, locally and from the cloud backup, without
     * touching the account itself. */
    suspend fun deleteAllData() {
        taskRepository.deleteAllTasksAndTags()
        journalRepository.deleteAllNotes()
    }

    /**
     * Deletes all Task/Journal data, then permanently deletes the Firebase Auth account.
     * Firebase requires a *recent* sign-in before it will delete an account; if the current
     * session is old enough to be rejected, this re-runs the same Google sign-in flow used
     * everywhere else in the app (never a separate login UI) to refresh it, then retries once.
     */
    suspend fun deleteAccount(context: Context) {
        deleteAllData()
        try {
            AuthRepository.deleteAccount()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            val idToken = AuthRepository.requestGoogleIdToken(context)
            AuthRepository.signInWithGoogleIdToken(idToken)
            AuthRepository.deleteAccount()
        }
    }
}
