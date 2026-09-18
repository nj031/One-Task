package com.nj031.onetask.data.reminder

import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.toRepeatDaysString
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Plain JUnit tests for [ReminderScheduler] - safe to run without Robolectric/Android since the
 * function under test has zero Android dependencies (see that file's own doc comment). */
class ReminderSchedulerTest {
    private val zone = ZoneId.systemDefault()

    private fun task(
        date: LocalDate,
        reminderMinuteOfDay: Int? = null,
        reminderDate: LocalDate? = null,
        repeat: TaskRepeat = TaskRepeat.NONE,
        repeatDays: Set<DayOfWeek> = emptySet()
    ) = TaskEntity(
        name = "Test task",
        date = date.toEpochDay(),
        reminderMinuteOfDay = reminderMinuteOfDay,
        reminderEpochDay = reminderDate?.toEpochDay(),
        repeat = repeat,
        repeatDays = repeatDays.toRepeatDaysString(),
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun atZone(date: LocalDate, minuteOfDay: Int): Long =
        LocalDateTime.of(date, LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `no reminder configured returns null`() {
        val today = LocalDate.of(2026, 9, 18)
        val t = task(date = today)
        val result = ReminderScheduler.computeNextOccurrence(t, atZone(today, 12 * 60), zone = zone)
        assertNull(result)
    }

    @Test
    fun `non-repeating reminder in the future is scheduled`() {
        val today = LocalDate.of(2026, 9, 18)
        val t = task(date = today, reminderMinuteOfDay = 18 * 60, reminderDate = today)
        val now = atZone(today, 10 * 60)
        val result = ReminderScheduler.computeNextOccurrence(t, now, zone = zone)
        assertEquals(today.toEpochDay(), result?.epochDay)
        assertEquals(atZone(today, 18 * 60), result?.fireAtEpochMillis)
    }

    @Test
    fun `non-repeating reminder already in the past returns null`() {
        val today = LocalDate.of(2026, 9, 18)
        val t = task(date = today, reminderMinuteOfDay = 9 * 60, reminderDate = today)
        val now = atZone(today, 17 * 60) // 5pm, reminder was for 9am
        val result = ReminderScheduler.computeNextOccurrence(t, now, zone = zone)
        assertNull(result)
    }

    @Test
    fun `non-repeating reminder already completed returns null`() {
        val today = LocalDate.of(2026, 9, 18)
        val t = task(date = today, reminderMinuteOfDay = 18 * 60, reminderDate = today)
        val now = atZone(today, 10 * 60)
        val result = ReminderScheduler.computeNextOccurrence(
            t, now, completedOccurrenceDates = setOf(today.toEpochDay()), zone = zone
        )
        assertNull(result)
    }

    @Test
    fun `daily reminder respects the task's own start date`() {
        val start = LocalDate.of(2026, 9, 20)
        val t = task(date = start, reminderMinuteOfDay = 8 * 60, repeat = TaskRepeat.DAILY)
        // "now" is two days before the series even starts - the first applicable occurrence must
        // still be the start date, never earlier.
        val now = atZone(start.minusDays(2), 0)
        val result = ReminderScheduler.computeNextOccurrence(t, now, zone = zone)
        assertEquals(start.toEpochDay(), result?.epochDay)
    }

    @Test
    fun `daily reminder skips an already-completed today and finds tomorrow`() {
        val today = LocalDate.of(2026, 9, 18)
        val t = task(date = today, reminderMinuteOfDay = 18 * 60, repeat = TaskRepeat.DAILY)
        val now = atZone(today, 10 * 60)
        val result = ReminderScheduler.computeNextOccurrence(
            t, now, completedOccurrenceDates = setOf(today.toEpochDay()), zone = zone
        )
        assertEquals(today.plusDays(1).toEpochDay(), result?.epochDay)
    }

    @Test
    fun `daily reminder whose time already passed today rolls to tomorrow`() {
        val today = LocalDate.of(2026, 9, 18)
        val t = task(date = today, reminderMinuteOfDay = 9 * 60, repeat = TaskRepeat.DAILY)
        val now = atZone(today, 17 * 60) // past today's 9am fire time
        val result = ReminderScheduler.computeNextOccurrence(t, now, zone = zone)
        assertEquals(today.plusDays(1).toEpochDay(), result?.epochDay)
    }

    @Test
    fun `select days reminder started on a non-matching Friday never fires on Friday`() {
        // Matches the spec's own example: start date = Friday, selected days = Mon/Tue/Wed/Sun,
        // reminder time = 6pm - Friday itself must never receive a reminder.
        val friday = LocalDate.of(2026, 9, 18)
        check(friday.dayOfWeek == DayOfWeek.FRIDAY)
        val selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)
        val t = task(date = friday, reminderMinuteOfDay = 18 * 60, repeat = TaskRepeat.SELECT_DAYS, repeatDays = selectedDays)
        val now = atZone(friday, 0)
        val result = ReminderScheduler.computeNextOccurrence(t, now, zone = zone)
        // The very next occurrence must be Sunday (2 days after Friday), never Friday itself.
        val expectedSunday = friday.plusDays(2)
        check(expectedSunday.dayOfWeek == DayOfWeek.SUNDAY)
        assertEquals(expectedSunday.toEpochDay(), result?.epochDay)
    }

    @Test
    fun `select days with empty day set never schedules anything`() {
        val today = LocalDate.of(2026, 9, 18)
        val t = task(date = today, reminderMinuteOfDay = 18 * 60, repeat = TaskRepeat.SELECT_DAYS, repeatDays = emptySet())
        val now = atZone(today, 0)
        val result = ReminderScheduler.computeNextOccurrence(t, now, zone = zone)
        assertNull(result)
    }
}
