package com.gps.zazor

import com.gps.zazor.utils.time.PhotoClock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Guards the two date-format bugs the stamp used to carry: Joda's `YYYY` (ISO week-year) instead
 * of `yyyy`, and `hh` (12-hour, with no AM/PM marker) instead of `HH`.
 */
class PhotoClockTest {

    private fun instantOf(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(ZoneId.systemDefault()).toInstant()

    @Test
    fun `date uses the calendar year, not the ISO week-year`() {
        // 29 Dec 2025 falls in ISO week 1 of week-year 2026: "YYYY" would print 2026 here.
        assertEquals("29.12.2025", PhotoClock.formatDate(instantOf(2025, 12, 29, 12, 0)))
    }

    @Test
    fun `date keeps the calendar year on an ordinary day too`() {
        assertEquals("03.09.2026", PhotoClock.formatDate(instantOf(2026, 9, 3, 8, 5)))
    }

    @Test
    fun `afternoon time is rendered on a 24-hour clock`() {
        // With "hh" this printed 03:30 and an afternoon photo was indistinguishable from a morning one.
        assertEquals("15:30", PhotoClock.formatTime(instantOf(2026, 9, 3, 15, 30)))
    }

    @Test
    fun `midnight is zero, not twelve`() {
        assertEquals("00:07", PhotoClock.formatTime(instantOf(2026, 9, 3, 0, 7)))
    }

    @Test
    fun `gallery line combines date and time`() {
        assertEquals("03.09.2026, 15:30", PhotoClock.formatDateTime(instantOf(2026, 9, 3, 15, 30)))
    }
}
