package com.gps.zazor.utils.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date and time formatting for the note stamped onto a photo.
 *
 * Replaces Joda-Time: `java.time` is available on every supported device through core library
 * desugaring, so the app no longer ships a second date library plus its time-zone resource blob,
 * and no longer needs an init call in [android.app.Application] before any date can be read.
 */
object PhotoClock {

    /** `yyyy`, not Joda's `YYYY` - the latter is the ISO week-year and is off by a year at the turn of one. */
    private const val DATE_PATTERN = "dd.MM.yyyy"

    /** `HH`, not `hh`: 24-hour, so 3 PM does not read as 03:00. */
    private const val TIME_PATTERN = "HH:mm"

    private val dateFormatter = formatter(DATE_PATTERN)
    private val timeFormatter = formatter(TIME_PATTERN)
    private val listFormatter = formatter("$DATE_PATTERN, $TIME_PATTERN")

    fun now(): Instant = Instant.now()

    fun formatDate(instant: Instant): String = dateFormatter.format(instant)

    fun formatTime(instant: Instant): String = timeFormatter.format(instant)

    /** Single line used in the gallery row. */
    fun formatDateTime(instant: Instant): String = listFormatter.format(instant)

    private fun formatter(pattern: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).withZone(ZoneId.systemDefault())
}
