package com.nj031.onetask.data.reminder

import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.matchesRecurrenceOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** How far ahead (in days) [ReminderScheduler.computeNextOccurrence] will search for the next
 * applicable SELECT_DAYS/DAILY occurrence before giving up - generous enough to always find at
 * least one match for any non-empty repeatDays set well within a year, while still terminating
 * for a pathological state (e.g. a corrupted/empty repeatDays on a SELECT_DAYS task) instead of
 * looping forever. */
private const val MAX_LOOKAHEAD_DAYS = 366

/** The next reminder this task should fire: an absolute fire time plus the calendar day (epoch
 * day) of the occurrence it belongs to - the latter is what a fired reminder needs to know which
 * specific day's completion status to check (see ReminderManager/ReminderReceiver). */
data class ReminderOccurrence(val fireAtEpochMillis: Long, val epochDay: Long)

/**
 * Pure scheduling math for Task Reminders - deliberately free of any Android/Room dependency so
 * it's plain-JUnit-testable. Given a task and "now", decides the single next reminder moment
 * that should be scheduled, reusing the exact same recurrence rules ([matchesRecurrenceOn]) the
 * rest of the app already uses to decide which calendar days a recurring task is due on, rather
 * than inventing a second definition of "which days this task recurs on".
 *
 * Only ever produces at most one upcoming occurrence at a time: the caller (ReminderManager)
 * schedules exactly that one via AlarmManager, and re-invokes this function to compute the next
 * one once it fires - a task/series never has more than one outstanding scheduled alarm.
 */
object ReminderScheduler {
    fun computeNextOccurrence(
        task: TaskEntity,
        fromEpochMillis: Long,
        completedOccurrenceDates: Set<Long> = emptySet(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ReminderOccurrence? {
        val minuteOfDay = task.reminderMinuteOfDay ?: return null
        if (minuteOfDay !in 0..1439) return null

        fun fireAtFor(epochDay: Long): Long = LocalDateTime.of(
            LocalDate.ofEpochDay(epochDay),
            LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        ).atZone(zone).toInstant().toEpochMilli()

        return when (task.repeat) {
            TaskRepeat.NONE -> {
                val epochDay = task.reminderEpochDay ?: return null
                if (epochDay in completedOccurrenceDates) return null
                val fireAt = fireAtFor(epochDay)
                if (fireAt <= fromEpochMillis) null else ReminderOccurrence(fireAt, epochDay)
            }
            TaskRepeat.DAILY, TaskRepeat.SELECT_DAYS -> {
                val todayEpochDay = Instant.ofEpochMilli(fromEpochMillis).atZone(zone).toLocalDate().toEpochDay()
                var candidate = maxOf(task.date, todayEpochDay)
                var result: ReminderOccurrence? = null
                var stepsTaken = 0
                while (stepsTaken < MAX_LOOKAHEAD_DAYS && result == null) {
                    if (task.matchesRecurrenceOn(candidate) && candidate !in completedOccurrenceDates) {
                        val fireAt = fireAtFor(candidate)
                        if (fireAt > fromEpochMillis) {
                            result = ReminderOccurrence(fireAt, candidate)
                        }
                    }
                    candidate += 1
                    stepsTaken += 1
                }
                result
            }
        }
    }
}
