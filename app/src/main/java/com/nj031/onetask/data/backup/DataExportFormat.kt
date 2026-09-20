package com.nj031.onetask.data.backup

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
    put("priority", priority.name)
    put("reminderMinuteOfDay", reminderMinuteOfDay ?: JSONObject.NULL)
    put("reminderEpochDay", reminderEpochDay ?: JSONObject.NULL)
    put("repeat", repeat.name)
    put("repeatDays", repeatDays)
    put("seriesId", seriesId ?: JSONObject.NULL)
    put("tag", tag ?: JSONObject.NULL)
    put("categoryId", categoryId ?: JSONObject.NULL)
    put("postponeIfIncomplete", postponeIfIncomplete)
    put("status", status.name)
    put("timerEndAtMillis", timerEndAtMillis ?: JSONObject.NULL)
    put("timerRemainingMillis", timerRemainingMillis ?: JSONObject.NULL)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("successCondition", successCondition.name)
    put("successConditionThreshold", successConditionThreshold ?: JSONObject.NULL)
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
        // Absent from any backup file written before the Priority feature - default to NONE
        // (no priority selected), exactly like a brand-new task without a chosen priority.
        priority = runCatching { TaskPriority.valueOf(getString("priority")) }.getOrDefault(TaskPriority.NONE),
        // Absent from any backup file written before the Reminder feature - default to null
        // (no reminder configured), exactly like a brand-new task without one.
        reminderMinuteOfDay = if (isNull("reminderMinuteOfDay")) null else getInt("reminderMinuteOfDay"),
        reminderEpochDay = if (isNull("reminderEpochDay")) null else getLong("reminderEpochDay"),
        // Absent from any backup file written before the Repeat fix (or naming a repeat option
        // that fix retired - WEEKLY/MONTHLY) - default to NONE, exactly like an unrecognized
        // status/note type elsewhere in this file already does, rather than failing the restore.
        repeat = runCatching { TaskRepeat.valueOf(getString("repeat")) }.getOrDefault(TaskRepeat.NONE),
        repeatDays = optString("repeatDays", ""),
        seriesId = if (isNull("seriesId")) null else getString("seriesId"),
        tag = if (isNull("tag")) null else getString("tag"),
        // Absent from any backup file written before Category existed - default to null ("No
        // Category"), exactly like a brand-new task without one.
        categoryId = if (has("categoryId") && !isNull("categoryId")) getString("categoryId") else null,
        postponeIfIncomplete = optBoolean("postponeIfIncomplete", true),
        status = runCatching { TaskStatus.valueOf(getString("status")) }.getOrDefault(TaskStatus.NOT_STARTED),
        timerEndAtMillis = if (isNull("timerEndAtMillis")) null else getLong("timerEndAtMillis"),
        timerRemainingMillis = if (isNull("timerRemainingMillis")) null else getLong("timerRemainingMillis"),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt"),
        // Absent from any backup file written before the Success Condition feature - default to
        // ALL/null, exactly like a brand-new task without an explicit choice.
        successCondition = runCatching { SuccessCondition.valueOf(getString("successCondition")) }
            .getOrDefault(SuccessCondition.ALL),
        successConditionThreshold = if (has("successConditionThreshold") && !isNull("successConditionThreshold")) {
            getInt("successConditionThreshold")
        } else {
            null
        }
    )
}

private fun JournalNoteEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("content", content)
    put("noteType", noteType.name)
    put(
        "checklistItems",
        JSONArray(
            checklistItems.map { item ->
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("checked", item.checked)
                }
            }
        )
    )
    put("journalDate", journalDate)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("status", status.name)
}

// noteType/checklistItems are absent from any backup file written before the Notes redesign -
// default to a plain text note (TEXT, no checklist items) so restoring an older backup still
// brings those notes back intact.
private fun JSONObject.toNoteEntity(): JournalNoteEntity {
    val checklistArray = optJSONArray("checklistItems")
    val checklistItems = if (checklistArray != null) {
        (0 until checklistArray.length()).map { index ->
            val entry = checklistArray.getJSONObject(index)
            ChecklistItem(
                id = entry.getString("id"),
                text = entry.getString("text"),
                checked = entry.optBoolean("checked", false)
            )
        }
    } else {
        emptyList()
    }
    return JournalNoteEntity(
        id = getString("id"),
        title = getString("title"),
        content = getString("content"),
        noteType = runCatching { JournalNoteType.valueOf(optString("noteType", JournalNoteType.TEXT.name)) }
            .getOrDefault(JournalNoteType.TEXT),
        checklistItems = checklistItems,
        journalDate = getLong("journalDate"),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt"),
        status = runCatching { JournalNoteStatus.valueOf(getString("status")) }.getOrDefault(JournalNoteStatus.ACTIVE)
    )
}
