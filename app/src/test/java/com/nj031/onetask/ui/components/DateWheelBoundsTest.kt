package com.nj031.onetask.ui.components

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Plain JUnit tests for the pure min/max-date bounding functions [OneTaskDatePickerDialog]'s
 * wheels are built from - the actual "never let an out-of-range date be constructed" mechanism
 * behind Date of Birth's future-date exclusion and Jump to Date's wide, effectively-unrestricted
 * range. Safe to run without Robolectric/Android since these are plain java.time functions. */
class DateWheelBoundsTest {
    private val minDate = LocalDate.of(2000, 3, 10)
    private val maxDate = LocalDate.of(2026, 9, 19)

    @Test
    fun `year range spans exactly minDate year to maxDate year`() {
        assertEquals(2000..2026, yearWheelRange(minDate, maxDate))
    }

    @Test
    fun `month range is unrestricted for a year strictly between the two boundary years`() {
        assertEquals(1..12, monthWheelRange(2013, minDate, maxDate))
    }

    @Test
    fun `month range is floored at the min year own boundary month`() {
        assertEquals(3..12, monthWheelRange(2000, minDate, maxDate))
    }

    @Test
    fun `month range is capped at the max year own boundary month`() {
        assertEquals(1..9, monthWheelRange(2026, minDate, maxDate))
    }

    @Test
    fun `day range is unrestricted for a month strictly inside the boundary years`() {
        assertEquals(1..30, dayWheelRange(2013, 4, minDate, maxDate))
    }

    @Test
    fun `day range is floored at the min date own day in the min boundary month`() {
        assertEquals(10..31, dayWheelRange(2000, 3, minDate, maxDate))
    }

    @Test
    fun `day range is capped at the max date own day in the max boundary month`() {
        assertEquals(1..19, dayWheelRange(2026, 9, minDate, maxDate))
    }

    @Test
    fun `day range never exceeds a short month actual length even with no upper bound in play`() {
        // February 2013 sits strictly between the two boundary years, so only the calendar's own
        // 28-day length should cap it - not some other stale value.
        assertEquals(1..28, dayWheelRange(2013, 2, minDate, maxDate))
    }

    @Test
    fun `a same-day min and max collapses every wheel to that single value`() {
        val onlyDay = LocalDate.of(2026, 9, 19)
        assertEquals(2026..2026, yearWheelRange(onlyDay, onlyDay))
        assertEquals(9..9, monthWheelRange(2026, onlyDay, onlyDay))
        assertEquals(19..19, dayWheelRange(2026, 9, onlyDay, onlyDay))
    }

    @Test
    fun `clamping a date already inside the bounds returns it unchanged`() {
        val inside = LocalDate.of(2015, 6, 1)
        assertEquals(inside, clampDateToBounds(inside, minDate, maxDate))
    }

    @Test
    fun `clamping a date before minDate pulls it up to minDate`() {
        val before = LocalDate.of(1990, 1, 1)
        assertEquals(minDate, clampDateToBounds(before, minDate, maxDate))
    }

    @Test
    fun `clamping a date after maxDate pulls it down to maxDate`() {
        val after = LocalDate.of(2030, 1, 1)
        assertEquals(maxDate, clampDateToBounds(after, minDate, maxDate))
    }
}
