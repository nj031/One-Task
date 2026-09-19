package com.nj031.onetask.data.sync

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.nj031.onetask.data.journal.ChecklistItem
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.journal.JournalNoteStatus
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.SuccessCondition
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskPriority
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.TaskStatus
import java.io.File
import kotlinx.coroutines.tasks.await

/** The signed-in account's cloud-backed Appearance settings (Display Mode + Color Theme), plus
 * when they were last changed - see [CloudBackupRepository.pushAppearance]/[pullAppearance]. */
data class CloudAppearance(val displayMode: String, val colorTheme: String, val updatedAt: Long)

/** The signed-in account's cloud-backed General Settings. Deliberately excludes
 * "last cloud backup at" (device/install bookkeeping, not a user preference - see
 * GeneralSettingsRepository's own KEY_LAST_CLOUD_BACKUP_AT). */
data class CloudGeneralSettings(
    val startScreen: String,
    val defaultTimerMinutes: Int?,
    val defaultTag: String?,
    val defaultPostponeIfIncomplete: Boolean,
    val focusSessionNotificationsEnabled: Boolean,
    val focusSessionCompleteEnabled: Boolean,
    val weekStartDay: String,
    val timeFormat: String,
    val hapticFeedbackEnabled: Boolean,
    val notesViewMode: String,
    val updatedAt: Long
)

/** The signed-in account's cloud-backed Profile fields. Deliberately excludes the photo, which
 * is backed by Firebase Storage instead (see [CloudBackupRepository.uploadProfilePhoto]) - a
 * binary blob doesn't belong in a Firestore document. */
data class CloudProfile(
    val name: String,
    val dateOfBirth: Long?,
    val gender: String?,
    val updatedAt: Long
)

// Pure, Firebase-SDK-free path builders - the actual account-isolation boundary every
// Firestore/Storage read or write in this file goes through. Kept as plain, internal (unit-
// testable from app/src/test - see CloudPathsTest) string functions rather than inline Firebase
// calls, so "two different uids always resolve to two different, non-overlapping paths" and "the
// given uid is always literally part of the path" can be verified directly, without needing a
// live Firebase SDK/emulator. `firestore.collection(path)` accepts a slash-delimited path string
// exactly as it would a chain of .document(id).collection(name) calls - this is standard,
// documented Firestore SDK behavior, not a new convention.
internal fun tasksPath(uid: String) = "users/$uid/tasks"
internal fun notesPath(uid: String) = "users/$uid/notes"
internal fun tagsPath(uid: String) = "users/$uid/tags"
internal fun labelsPath(uid: String) = "users/$uid/labels"
internal fun accountPath(uid: String) = "users/$uid/account"
internal fun profilePhotoPath(uid: String) = "profile_photos/$uid.jpg"

/** Which side of an account-settings restore (Appearance/General Settings/Profile - the single-
 * document blobs, as opposed to Tasks/Notes/Tags/Labels' own per-item merge) should win, given
 * the local copy's own updatedAt and the cloud copy's updatedAt (null when no cloud document
 * exists yet for this account). Pure and Firebase-SDK-free - see SyncDecisionTest. Mirrors the
 * plain "cloud wins when present" rule [AuthViewModel.syncAfterSignIn] already uses for Tasks/
 * Notes/Tags, refined with a recency check so a local edit made just before this sync ran (and
 * therefore not yet reflected in the cloud) is never clobbered by an older cloud copy. */
internal enum class SyncDecision { PUSH_LOCAL, APPLY_REMOTE }

internal fun decideSync(localUpdatedAt: Long, cloudUpdatedAt: Long?): SyncDecision =
    if (cloudUpdatedAt != null && cloudUpdatedAt >= localUpdatedAt) SyncDecision.APPLY_REMOTE else SyncDecision.PUSH_LOCAL

/**
 * Mirrors local Room data (and, since the account-persistence work below, local
 * SharedPreferences-backed settings and the local profile photo file) to Cloud Firestore/Storage
 * under users/{uid}/... for automatic backup whenever a user is signed in - this is the durable,
 * installation-independent source of truth for every account-owned piece of state: local
 * Room/SharedPreferences/files remain the fast local cache/read path, but are never the only
 * copy of anything account-owned.
 *
 * Per-item write/delete calls (pushTask/pushNote/pushTag/pushLabel/delete*) are genuinely
 * awaited (they were previously fire-and-forget, unawaited Firestore Task calls - the root cause
 * of account data silently failing to survive an uninstall that happened before an unconfirmed
 * write had reached the server). A failure from one of these is caught inside
 * [runFirestoreWrite] rather than propagated: the local Room/SharedPreferences write this always
 * follows has already committed by the time a cloud write could fail, so a cloud hiccup must
 * never crash the app, roll back the already-applied local change, or block the UI - and
 * Firestore's own offline persistence has already queued the write and will retry it
 * automatically once connectivity returns regardless of whether this call is awaited. Bulk
 * pull/push calls (pushAllTasks, pullTasks, ...), used only by the explicit account-restore/
 * backup flows in AuthViewModel/DataPrivacyViewModel, intentionally keep propagating failure
 * (unchanged from before) - those callers need to know whether a sync genuinely completed.
 */
object CloudBackupRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val uid: String? get() = auth.currentUser?.uid

    private fun tasksCollection(uid: String) = firestore.collection(tasksPath(uid))

    private fun notesCollection(uid: String) = firestore.collection(notesPath(uid))

    private fun tagsCollection(uid: String) = firestore.collection(tagsPath(uid))

    /** Mirrors [tagsCollection] exactly - the Notes equivalent of Custom Tags (see
     * [com.nj031.onetask.data.journal.NoteLabelEntity]'s own doc comment for why the two are
     * kept as separate tables/collections despite being the same shape). */
    private fun labelsCollection(uid: String) = firestore.collection(labelsPath(uid))

    /** The small, single-document account-settings blobs (Appearance/General Settings/Profile) -
     * kept under their own "account" subcollection rather than as fields directly on the
     * users/{uid} document itself, so each can be read/written independently without ever
     * touching the others, the same isolation tasks/notes/tags/labels already get from being
     * separate subcollections. */
    private fun accountDocument(uid: String, key: String) = firestore.collection(accountPath(uid)).document(key)

    /** Deterministic, UID-scoped Storage path for the signed-in account's profile photo - no
     * separate "photo URL" needs to be stored anywhere (in Firestore or otherwise): the path is
     * always derivable from the uid alone, the same convention
     * [com.nj031.onetask.data.profile.ProfilePhotoStorage] already uses for the local file. */
    private fun profilePhotoRef(uid: String) = storage.reference.child(profilePhotoPath(uid))

    /** Runs a single Firestore write/delete, awaiting its actual server confirmation rather than
     * firing it and immediately returning (the fix for the root cause described in this object's
     * own doc comment) - but swallows a failure rather than throwing, so a transient/offline
     * failure on a routine per-item mutation can never crash the caller or block the local
     * change that already happened. See the class doc for why this asymmetry (per-item writes
     * swallow; bulk sync writes still throw) is intentional. */
    private suspend fun runFirestoreWrite(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            // Local state already committed before this runs; Firestore's own offline queue has
            // already recorded the write and will retry it automatically once connectivity
            // returns. Nothing further to do here - there is no existing error-reporting/retry
            // UI in this app to surface this to, and building one is out of scope for this fix.
        }
    }

    suspend fun pushTask(task: TaskEntity) {
        val currentUid = uid ?: return
        runFirestoreWrite { tasksCollection(currentUid).document(task.id).set(task.toFirestoreMap()).await() }
    }

    suspend fun deleteTask(taskId: String) {
        val currentUid = uid ?: return
        runFirestoreWrite { tasksCollection(currentUid).document(taskId).delete().await() }
    }

    /** Custom tags are account-owned data (see [pullTags]) - a tag's own name is both its
     * identity and its entire content, so the name is used directly as the Firestore document
     * id, the same way local Room's task_tags table already uses it as the primary key. */
    suspend fun pushTag(name: String) {
        val currentUid = uid ?: return
        runFirestoreWrite { tagsCollection(currentUid).document(name).set(mapOf("name" to name)).await() }
    }

    suspend fun deleteTag(name: String) {
        val currentUid = uid ?: return
        runFirestoreWrite { tagsCollection(currentUid).document(name).delete().await() }
    }

    suspend fun pushNote(note: JournalNoteEntity) {
        val currentUid = uid ?: return
        runFirestoreWrite { notesCollection(currentUid).document(note.id).set(note.toFirestoreMap()).await() }
    }

    suspend fun deleteNote(noteId: String) {
        val currentUid = uid ?: return
        runFirestoreWrite { notesCollection(currentUid).document(noteId).delete().await() }
    }

    /** Mirrors [pushTag] exactly. */
    suspend fun pushLabel(name: String) {
        val currentUid = uid ?: return
        runFirestoreWrite { labelsCollection(currentUid).document(name).set(mapOf("name" to name)).await() }
    }

    suspend fun pushAppearance(appearance: CloudAppearance) {
        val currentUid = uid ?: return
        runFirestoreWrite {
            accountDocument(currentUid, "appearance").set(
                mapOf(
                    "displayMode" to appearance.displayMode,
                    "colorTheme" to appearance.colorTheme,
                    "updatedAt" to appearance.updatedAt
                )
            ).await()
        }
    }

    suspend fun pushGeneralSettings(settings: CloudGeneralSettings) {
        val currentUid = uid ?: return
        runFirestoreWrite {
            accountDocument(currentUid, "generalSettings").set(
                mapOf(
                    "startScreen" to settings.startScreen,
                    "defaultTimerMinutes" to settings.defaultTimerMinutes,
                    "defaultTag" to settings.defaultTag,
                    "defaultPostponeIfIncomplete" to settings.defaultPostponeIfIncomplete,
                    "focusSessionNotificationsEnabled" to settings.focusSessionNotificationsEnabled,
                    "focusSessionCompleteEnabled" to settings.focusSessionCompleteEnabled,
                    "weekStartDay" to settings.weekStartDay,
                    "timeFormat" to settings.timeFormat,
                    "hapticFeedbackEnabled" to settings.hapticFeedbackEnabled,
                    "notesViewMode" to settings.notesViewMode,
                    "updatedAt" to settings.updatedAt
                )
            ).await()
        }
    }

    suspend fun pushProfile(profile: CloudProfile) {
        val currentUid = uid ?: return
        runFirestoreWrite {
            accountDocument(currentUid, "profile").set(
                mapOf(
                    "name" to profile.name,
                    "dateOfBirth" to profile.dateOfBirth,
                    "gender" to profile.gender,
                    "updatedAt" to profile.updatedAt
                )
            ).await()
        }
    }

    /** Uploads [localFile] as the signed-in account's profile photo, at a deterministic
     * UID-scoped Storage path only that account's own uid ever resolves to - never a
     * shared/global path. Swallows failure the same way [runFirestoreWrite] does for Firestore:
     * the local copy (already saved by [com.nj031.onetask.data.profile.ProfilePhotoStorage]
     * before this is ever called) remains the source of truth for the current session either
     * way. */
    suspend fun uploadProfilePhoto(localFile: File) {
        val currentUid = uid ?: return
        try {
            profilePhotoRef(currentUid).putFile(Uri.fromFile(localFile)).await()
        } catch (_: Exception) {
            // Same reasoning as runFirestoreWrite - local file already exists; nothing to roll
            // back, nothing further to do here.
        }
    }

    /** Removes the signed-in account's cloud profile photo - called when the user explicitly
     * removes their photo, so a later reinstall doesn't resurrect a photo they deleted. A
     * missing remote object (nothing was ever uploaded) is not an error. */
    suspend fun deleteProfilePhoto() {
        val currentUid = uid ?: return
        try {
            profilePhotoRef(currentUid).delete().await()
        } catch (e: StorageException) {
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        } catch (_: Exception) {
            // Same reasoning as runFirestoreWrite.
        }
    }

    /** Downloads the signed-in account's cloud profile photo into [destinationFile] (the same
     * UID-scoped local file [com.nj031.onetask.data.profile.ProfilePhotoStorage] already uses),
     * returning true only if a photo actually existed and was written. A 404 (this account has
     * never uploaded one) is the expected, common case for any account that hasn't set a photo -
     * not an error, and never deletes/clears [destinationFile] on any failure, so a genuinely
     * offline restore attempt can't wipe out a local photo that was somehow already there. */
    suspend fun downloadProfilePhoto(destinationFile: File): Boolean {
        val currentUid = uid ?: return false
        return try {
            profilePhotoRef(currentUid).getFile(destinationFile).await()
            true
        } catch (_: Exception) {
            // Covers both "this account has never uploaded a photo" (StorageException,
            // ERROR_OBJECT_NOT_FOUND - the common case) and any other transient failure. Either
            // way, destinationFile is never touched/cleared on failure - see this function's own
            // doc comment.
            false
        }
    }

    suspend fun pullTasks(): List<TaskEntity> {
        val currentUid = uid ?: return emptyList()
        return tasksCollection(currentUid).get().await().documents.mapNotNull { it.toTaskEntity() }
    }

    suspend fun pullNotes(): List<JournalNoteEntity> {
        val currentUid = uid ?: return emptyList()
        return notesCollection(currentUid).get().await().documents.mapNotNull { it.toJournalNoteEntity() }
    }

    /** The signed-in account's own custom tags, previously backed up from this or any other
     * device - restores them on a fresh install/reinstall or a first sign-in on a new device. */
    suspend fun pullTags(): List<String> {
        val currentUid = uid ?: return emptyList()
        return tagsCollection(currentUid).get().await().documents.mapNotNull { it.getString("name") }
    }

    /** Mirrors [pullTags] exactly. */
    suspend fun pullLabels(): List<String> {
        val currentUid = uid ?: return emptyList()
        return labelsCollection(currentUid).get().await().documents.mapNotNull { it.getString("name") }
    }

    suspend fun pullAppearance(): CloudAppearance? {
        val currentUid = uid ?: return null
        val doc = accountDocument(currentUid, "appearance").get().await()
        if (!doc.exists()) return null
        val displayMode = doc.getString("displayMode") ?: return null
        val colorTheme = doc.getString("colorTheme") ?: return null
        return CloudAppearance(displayMode, colorTheme, doc.getLong("updatedAt") ?: 0L)
    }

    suspend fun pullGeneralSettings(): CloudGeneralSettings? {
        val currentUid = uid ?: return null
        val doc = accountDocument(currentUid, "generalSettings").get().await()
        if (!doc.exists()) return null
        val startScreen = doc.getString("startScreen") ?: return null
        val weekStartDay = doc.getString("weekStartDay") ?: return null
        val timeFormat = doc.getString("timeFormat") ?: return null
        val notesViewMode = doc.getString("notesViewMode") ?: return null
        return CloudGeneralSettings(
            startScreen = startScreen,
            defaultTimerMinutes = (doc.get("defaultTimerMinutes") as? Long)?.toInt(),
            defaultTag = doc.getString("defaultTag"),
            defaultPostponeIfIncomplete = doc.getBoolean("defaultPostponeIfIncomplete") ?: true,
            focusSessionNotificationsEnabled = doc.getBoolean("focusSessionNotificationsEnabled") ?: true,
            focusSessionCompleteEnabled = doc.getBoolean("focusSessionCompleteEnabled") ?: true,
            weekStartDay = weekStartDay,
            timeFormat = timeFormat,
            hapticFeedbackEnabled = doc.getBoolean("hapticFeedbackEnabled") ?: true,
            notesViewMode = notesViewMode,
            updatedAt = doc.getLong("updatedAt") ?: 0L
        )
    }

    suspend fun pullProfile(): CloudProfile? {
        val currentUid = uid ?: return null
        val doc = accountDocument(currentUid, "profile").get().await()
        if (!doc.exists()) return null
        val name = doc.getString("name") ?: return null
        return CloudProfile(
            name = name,
            dateOfBirth = doc.getLong("dateOfBirth"),
            gender = doc.getString("gender"),
            updatedAt = doc.getLong("updatedAt") ?: 0L
        )
    }

    suspend fun pushAllTasks(tasks: List<TaskEntity>) {
        val currentUid = uid ?: return
        if (tasks.isEmpty()) return
        val batch = firestore.batch()
        tasks.forEach { task -> batch.set(tasksCollection(currentUid).document(task.id), task.toFirestoreMap()) }
        batch.commit().await()
    }

    suspend fun pushAllNotes(notes: List<JournalNoteEntity>) {
        val currentUid = uid ?: return
        if (notes.isEmpty()) return
        val batch = firestore.batch()
        notes.forEach { note -> batch.set(notesCollection(currentUid).document(note.id), note.toFirestoreMap()) }
        batch.commit().await()
    }

    suspend fun pushAllTags(tags: List<String>) {
        val currentUid = uid ?: return
        if (tags.isEmpty()) return
        val batch = firestore.batch()
        tags.forEach { tag -> batch.set(tagsCollection(currentUid).document(tag), mapOf("name" to tag)) }
        batch.commit().await()
    }

    /** Mirrors [pushAllTags] exactly. */
    suspend fun pushAllLabels(labels: List<String>) {
        val currentUid = uid ?: return
        if (labels.isEmpty()) return
        val batch = firestore.batch()
        labels.forEach { label -> batch.set(labelsCollection(currentUid).document(label), mapOf("name" to label)) }
        batch.commit().await()
    }
}

private fun TaskEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "subtasks" to subtasks.map { mapOf("id" to it.id, "name" to it.name, "completed" to it.completed) },
    "timerMinutes" to timerMinutes,
    "date" to date,
    "priority" to priority.name,
    "reminderMinuteOfDay" to reminderMinuteOfDay,
    "reminderEpochDay" to reminderEpochDay,
    "repeat" to repeat.name,
    "repeatDays" to repeatDays,
    "seriesId" to seriesId,
    "tag" to tag,
    "postponeIfIncomplete" to postponeIfIncomplete,
    "status" to status.name,
    "timerEndAtMillis" to timerEndAtMillis,
    "timerRemainingMillis" to timerRemainingMillis,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "orderInAll" to orderInAll,
    "orderInProgress" to orderInProgress,
    "orderInDone" to orderInDone,
    "successCondition" to successCondition.name,
    "successConditionThreshold" to successConditionThreshold
)

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toTaskEntity(): TaskEntity? {
    val name = getString("name") ?: return null
    val date = getLong("date") ?: return null
    val createdAt = getLong("createdAt") ?: return null
    val updatedAt = getLong("updatedAt") ?: return null
    val subtasksRaw = get("subtasks") as? List<Map<String, Any?>> ?: emptyList()
    return TaskEntity(
        id = id,
        name = name,
        subtasks = subtasksRaw.mapNotNull { raw ->
            val subId = raw["id"] as? String ?: return@mapNotNull null
            val subName = raw["name"] as? String ?: return@mapNotNull null
            Subtask(id = subId, name = subName, completed = raw["completed"] as? Boolean ?: false)
        },
        timerMinutes = (get("timerMinutes") as? Long)?.toInt(),
        date = date,
        priority = getString("priority")?.let { runCatching { TaskPriority.valueOf(it) }.getOrNull() }
            ?: TaskPriority.NONE,
        reminderMinuteOfDay = (get("reminderMinuteOfDay") as? Long)?.toInt(),
        reminderEpochDay = get("reminderEpochDay") as? Long,
        repeat = getString("repeat")?.let { runCatching { TaskRepeat.valueOf(it) }.getOrNull() } ?: TaskRepeat.NONE,
        repeatDays = getString("repeatDays").orEmpty(),
        seriesId = getString("seriesId"),
        tag = getString("tag"),
        postponeIfIncomplete = getBoolean("postponeIfIncomplete") ?: true,
        status = getString("status")?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() } ?: TaskStatus.NOT_STARTED,
        timerEndAtMillis = getLong("timerEndAtMillis"),
        timerRemainingMillis = getLong("timerRemainingMillis"),
        createdAt = createdAt,
        updatedAt = updatedAt,
        // Absent from any task document written before task-order sync existed - default to
        // createdAt, exactly matching TaskEntity's own constructor default for a row that's
        // never been manually reordered (see TaskEntity.kt), so an old cloud document restores
        // to the same "falls back to creation order" state it already implicitly had.
        orderInAll = getLong("orderInAll") ?: createdAt,
        orderInProgress = getLong("orderInProgress") ?: createdAt,
        orderInDone = getLong("orderInDone") ?: createdAt,
        // Absent from any task document written before Success Condition existed - default to
        // ALL/null, exactly matching TaskEntity's own constructor defaults (see TaskEntity.kt).
        successCondition = getString("successCondition")?.let { runCatching { SuccessCondition.valueOf(it) }.getOrNull() }
            ?: SuccessCondition.ALL,
        successConditionThreshold = (get("successConditionThreshold") as? Long)?.toInt()
    )
}

private fun JournalNoteEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "content" to content,
    "noteType" to noteType.name,
    "checklistItems" to checklistItems.map { mapOf("id" to it.id, "text" to it.text, "checked" to it.checked) },
    "journalDate" to journalDate,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "status" to status.name,
    "label" to label
)

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toJournalNoteEntity(): JournalNoteEntity? {
    val title = getString("title") ?: return null
    val content = getString("content") ?: return null
    val journalDate = getLong("journalDate") ?: return null
    val createdAt = getLong("createdAt") ?: return null
    val updatedAt = getLong("updatedAt") ?: return null
    // Both fields are absent on any note document written before the Notes redesign - default
    // to a plain text note (TEXT, no checklist items) so those existing notes still load intact.
    val checklistItemsRaw = get("checklistItems") as? List<Map<String, Any?>> ?: emptyList()
    return JournalNoteEntity(
        id = id,
        title = title,
        content = content,
        noteType = getString("noteType")?.let { runCatching { JournalNoteType.valueOf(it) }.getOrNull() }
            ?: JournalNoteType.TEXT,
        checklistItems = checklistItemsRaw.mapNotNull { raw ->
            val itemId = raw["id"] as? String ?: return@mapNotNull null
            val text = raw["text"] as? String ?: return@mapNotNull null
            ChecklistItem(id = itemId, text = text, checked = raw["checked"] as? Boolean ?: false)
        },
        journalDate = journalDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        status = getString("status")?.let { runCatching { JournalNoteStatus.valueOf(it) }.getOrNull() }
            ?: JournalNoteStatus.ACTIVE,
        // Absent from any note document written before per-note labels were synced - default to
        // null (no label), exactly matching JournalNoteEntity's own constructor default.
        label = getString("label")
    )
}
