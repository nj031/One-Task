package com.nj031.onetask.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.journal.JournalNoteStatus
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.TaskStatus
import kotlinx.coroutines.tasks.await

/**
 * Mirrors local Room data to Cloud Firestore under users/{uid}/... for automatic backup
 * whenever a user is signed in. Push/delete calls are fire-and-forget: the Firestore SDK
 * queues writes locally (including while offline) and syncs them in the background, so
 * callers never block on network I/O for a local mutation to complete. No-ops entirely
 * when nobody is signed in.
 */
object CloudBackupRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val uid: String? get() = auth.currentUser?.uid

    private fun tasksCollection(uid: String) =
        firestore.collection("users").document(uid).collection("tasks")

    private fun notesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("notes")

    fun pushTask(task: TaskEntity) {
        val currentUid = uid ?: return
        tasksCollection(currentUid).document(task.id).set(task.toFirestoreMap())
    }

    fun deleteTask(taskId: String) {
        val currentUid = uid ?: return
        tasksCollection(currentUid).document(taskId).delete()
    }

    fun pushNote(note: JournalNoteEntity) {
        val currentUid = uid ?: return
        notesCollection(currentUid).document(note.id).set(note.toFirestoreMap())
    }

    fun deleteNote(noteId: String) {
        val currentUid = uid ?: return
        notesCollection(currentUid).document(noteId).delete()
    }

    suspend fun pullTasks(): List<TaskEntity> {
        val currentUid = uid ?: return emptyList()
        return tasksCollection(currentUid).get().await().documents.mapNotNull { it.toTaskEntity() }
    }

    suspend fun pullNotes(): List<JournalNoteEntity> {
        val currentUid = uid ?: return emptyList()
        return notesCollection(currentUid).get().await().documents.mapNotNull { it.toJournalNoteEntity() }
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
}

private fun TaskEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "subtasks" to subtasks.map { mapOf("id" to it.id, "name" to it.name, "completed" to it.completed) },
    "timerMinutes" to timerMinutes,
    "date" to date,
    "repeat" to repeat.name,
    "tag" to tag,
    "postponeIfIncomplete" to postponeIfIncomplete,
    "status" to status.name,
    "timerEndAtMillis" to timerEndAtMillis,
    "timerRemainingMillis" to timerRemainingMillis,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
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
        repeat = getString("repeat")?.let { runCatching { TaskRepeat.valueOf(it) }.getOrNull() } ?: TaskRepeat.NONE,
        tag = getString("tag"),
        postponeIfIncomplete = getBoolean("postponeIfIncomplete") ?: true,
        status = getString("status")?.let { runCatching { TaskStatus.valueOf(it) }.getOrNull() } ?: TaskStatus.NOT_STARTED,
        timerEndAtMillis = getLong("timerEndAtMillis"),
        timerRemainingMillis = getLong("timerRemainingMillis"),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun JournalNoteEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "content" to content,
    "journalDate" to journalDate,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "status" to status.name
)

private fun DocumentSnapshot.toJournalNoteEntity(): JournalNoteEntity? {
    val title = getString("title") ?: return null
    val content = getString("content") ?: return null
    val journalDate = getLong("journalDate") ?: return null
    val createdAt = getLong("createdAt") ?: return null
    val updatedAt = getLong("updatedAt") ?: return null
    return JournalNoteEntity(
        id = id,
        title = title,
        content = content,
        journalDate = journalDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        status = getString("status")?.let { runCatching { JournalNoteStatus.valueOf(it) }.getOrNull() }
            ?: JournalNoteStatus.ACTIVE
    )
}
