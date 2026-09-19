package com.nj031.onetask.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Plain JUnit tests for [minuteOfDayToWheelState]/[wheelStateToMinuteOfDay] - the pure
 * minutes-since-midnight <-> 12-hour-wheel conversion behind [OneTaskTimePickerDialog], covering
 * the classic 12-hour-clock edge cases (midnight, noon) a naive `hour % 12` conversion gets
 * wrong. Safe to run without Robolectric/Android - zero Android dependency. */
class TimeWheelStateTest {
    @Test
    fun `midnight is 12 AM, not 0 AM`() {
        val state = minuteOfDayToWheelState(0)
        assertEquals(TimeWheelState(hour12 = 12, minute = 0, isPm = false), state)
    }

    @Test
    fun `noon is 12 PM, not 0 PM`() {
        val state = minuteOfDayToWheelState(12 * 60)
        assertEquals(TimeWheelState(hour12 = 12, minute = 0, isPm = true), state)
    }

    @Test
    fun `one minute after midnight is 12-01 AM`() {
        val state = minuteOfDayToWheelState(1)
        assertEquals(TimeWheelState(hour12 = 12, minute = 1, isPm = false), state)
    }

    @Test
    fun `one minute before noon is 11-59 AM`() {
        val state = minuteOfDayToWheelState(11 * 60 + 59)
        assertEquals(TimeWheelState(hour12 = 11, minute = 59, isPm = false), state)
    }

    @Test
    fun `one minute after noon is 12-01 PM`() {
        val state = minuteOfDayToWheelState(12 * 60 + 1)
        assertEquals(TimeWheelState(hour12 = 12, minute = 1, isPm = true), state)
    }

    @Test
    fun `one minute before midnight is 11-59 PM`() {
        val state = minuteOfDayToWheelState(23 * 60 + 59)
        assertEquals(TimeWheelState(hour12 = 11, minute = 59, isPm = true), state)
    }

    @Test
    fun `9-00 AM round-trips exactly - the picker own default`() {
        val minuteOfDay = 9 * 60
        val state = minuteOfDayToWheelState(minuteOfDay)
        assertEquals(minuteOfDay, wheelStateToMinuteOfDay(state.hour12, state.minute, state.isPm))
    }

    @Test
    fun `12-00 AM (midnight) round-trips back to minute 0`() {
        assertEquals(0, wheelStateToMinuteOfDay(hour12 = 12, minute = 0, isPm = false))
    }

    @Test
    fun `12-00 PM (noon) round-trips back to minute 720`() {
        assertEquals(720, wheelStateToMinuteOfDay(hour12 = 12, minute = 0, isPm = true))
    }

    @Test
    fun `every minute of the day round-trips through the wheel representation unchanged`() {
        for (minuteOfDay in 0 until 24 * 60) {
            val state = minuteOfDayToWheelState(minuteOfDay)
            assertEquals(
                "minuteOfDay=$minuteOfDay",
                minuteOfDay,
                wheelStateToMinuteOfDay(state.hour12, state.minute, state.isPm)
            )
        }
    }
}
