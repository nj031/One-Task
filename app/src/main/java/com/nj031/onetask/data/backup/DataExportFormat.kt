package com.nj031.onetask.data.backup

import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.journal.JournalNoteStatus
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.TaskStatus
import org.json.JSONArray
import org.json.JSONObject

private const val EXPORT_FORMAT_VERSION = 1

/** Parsed contents of a One Task backup file, ready to be merged into local storage. */
data class ParsedBackup(
    val tasks: List<TaskEntity>,
    val notes: List<JournalNoteEntity>,
    val tags: List<String>
)

/**
 * Plain-JSON (org.json, already on the Android platform - no new dependency) export/import
 * format for Data & Privacy's Export/Restore. Deliberately independent of Room's own
 * TypeConverters and of CloudBackupRepository's Firestore document shape, so this format stays
 * stable even if either of those internal representations changes later.
 */
object DataExportFormat {
    fun toJson(tasks: List<TaskEntity>, notes: List<JournalNoteEntity>, tags: List<String>): String {
        val root = JSONObject()
        root.put("formatVersion", EXPORT_FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("tasks", JSONArray(tasks.map { it.toJson() }))
        root.put("notes", JSONArray(notes.map { it.toJson() }))
        root.put("tags", JSONArray(tags))
        return root.toString(2)
    }

    fun parse(json: String): ParsedBackup {
        val root = JSONObject(json)
        val tasks = root.optJSONArray("tasks")?.let { array ->
            (0 until array.length()).map { array.getJSONObject(it).toTaskEntity() }
        }.orEmpty()
        val notes = root.optJSONArray("notes")?.let { array ->
            (0 until array.length()).map { array.getJSONObject(it).toNoteEntity() }
        }.orEmpty()
        val tags = root.optJSONArray("tags")?.let { array ->
            (0 until array.length()).map { array.getString(it) }
        }.orEmpty()
        return ParsedBackup(tasks, notes, tags)
    }
}

private fun TaskEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put(
        "subtasks",
        JSONArray(
            subtasks.map { subtask ->
                JSONObject().apply {
                    put("id", subtask.id)
                    put("name", subtask.name)
                    put("completed", subtask.completed)
                }
            }
        )
    )
    put("timerMinutes", timerMinutes ?: JSONObject.NULL)
    put("date", date)
    put("repeat", repeat.name)
    put("tag", tag ?: JSONObject.NULL)
    put("postponeIfIncomplete", postponeIfIncomplete)
    put("status", status.name)
    put("timerEndAtMillis", timerEndAtMillis ?: JSONObject.NULL)
    put("timerRemainingMillis", timerRemainingMillis ?: JSONObject.NULL)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
}

private fun JSONObject.toTaskEntity(): TaskEntity {
    val subtasksArray = optJSONArray("subtasks")
    val subtasks = if (subtasksArray != null) {
        (0 until subtasksArray.length()).map { index ->
            val entry = subtasksArray.getJSONObject(index)
            Subtask(
                id = entry.getString("id"),
                name = entry.getString("name"),
                completed = entry.optBoolean("completed", false)
            )
        }
    } else {
        emptyList()
    }
    return TaskEntity(
        id = getString("id"),
        name = getString("name"),
        subtasks = subtasks,
        timerMinutes = if (isNull("timerMinutes")) null else getInt("timerMinutes"),
        date = getLong("date"),
        repeat = runCatching { TaskRepeat.valueOf(getString("repeat")) }.getOrDefault(TaskRepeat.NONE),
        tag = if (isNull("tag")) null else getString("tag"),
        postponeIfIncomplete = optBoolean("postponeIfIncomplete", true),
        status = runCatching { TaskStatus.valueOf(getString("status")) }.getOrDefault(TaskStatus.NOT_STARTED),
        timerEndAtMillis = if (isNull("timerEndAtMillis")) null else getLong("timerEndAtMillis"),
        timerRemainingMillis = if (isNull("timerRemainingMillis")) null else getLong("timerRemainingMillis"),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt")
    )
}

private fun JournalNoteEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("content", content)
    put("journalDate", journalDate)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("status", status.name)
}

private fun JSONObject.toNoteEntity(): JournalNoteEntity = JournalNoteEntity(
    id = getString("id"),
    title = getString("title"),
    content = getString("content"),
    journalDate = getLong("journalDate"),
    createdAt = getLong("createdAt"),
    updatedAt = getLong("updatedAt"),
    status = runCatching { JournalNoteStatus.valueOf(getString("status")) }.getOrDefault(JournalNoteStatus.ACTIVE)
)
