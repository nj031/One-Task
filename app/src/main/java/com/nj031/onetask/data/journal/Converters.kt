package com.nj031.onetask.data.journal

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: JournalNoteStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): JournalNoteStatus = JournalNoteStatus.valueOf(value)
}
