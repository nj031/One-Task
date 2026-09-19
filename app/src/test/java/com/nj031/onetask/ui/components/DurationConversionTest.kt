package com.nj031.onetask.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Plain JUnit tests for [durationMillisToWholeMinutes] - the conversion Add Task's Timer field
 * and Default Task Settings' Default Timer use to interpret [OneTaskDurationPickerDialog]'s
 * always-H/M/S output for their own minute-only stored fields, per the explicit "do not silently
 * corrupt existing minute-based stored values" requirement: this locks down exactly how seconds
 * get rounded away rather than leaving it as an undocumented implementation detail. */
class DurationConversionTest {
    @Test
    fun `an exact whole number of minutes converts unchanged`() {
        assertEquals(25, durationMillisToWholeMinutes(25 * 60_000L))
    }

    @Test
    fun `zero millis converts to zero minutes`() {
        assertEquals(0, durationMillisToWholeMinutes(0L))
    }

    @Test
    fun `a few seconds under a minute rounds down to zero`() {
        assertEquals(0, durationMillisToWholeMinutes(29_000L))
    }

    @Test
    fun `exactly half a minute over rounds up to the next minute`() {
        assertEquals(2, durationMillisToWholeMinutes(90_000L))
    }

    @Test
    fun `just under half a minute over rounds down`() {
        assertEquals(1, durationMillisToWholeMinutes(89_000L))
    }

    @Test
    fun `just under a full minute rounds up to that minute`() {
        assertEquals(1, durationMillisToWholeMinutes(59_999L))
    }
}
