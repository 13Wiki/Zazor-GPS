package com.gps.zazor.data.models

import androidx.annotation.StringRes
import com.gps.zazor.R
import java.util.Locale
import kotlin.math.abs

/**
 * How a coordinate is written for a person to read.
 *
 * Decimal degrees are what a phone and a navigator speak to each other; degrees, minutes and
 * seconds are what surveying documents, land records and older maps are written in, and someone
 * filing a report often has to match the form in front of them. Both say the same point.
 *
 * Only the reading changes. GPX and KML keep decimal degrees whatever is chosen here - those are
 * machine formats, and their specifications leave no room for a preference.
 */
enum class CoordinateFormat(@StringRes val titleRes: Int) {

    DEGREES(R.string.coordinate_format_degrees),
    DEGREES_MINUTES_SECONDS(R.string.coordinate_format_dms);

    fun format(value: Double): String = when (this) {
        DEGREES -> String.format(Locale.US, "%.6f", value)
        DEGREES_MINUTES_SECONDS -> {
            val absolute = abs(value)
            val degrees = absolute.toInt()
            val minutesTotal = (absolute - degrees) * MINUTES_IN_DEGREE
            val minutes = minutesTotal.toInt()
            val seconds = (minutesTotal - minutes) * SECONDS_IN_MINUTE
            val sign = if (value < 0) "-" else ""
            String.format(Locale.US, "%s%d°%02d'%04.1f\"", sign, degrees, minutes, seconds)
        }
    }

    /**
     * The format shown by example, for a settings row: "Degrees, minutes, seconds" pushed the row's
     * own title onto two lines, and a number says which is which in any language.
     */
    val sample: String get() = format(SAMPLE_COORDINATE)

    private companion object {

        const val SAMPLE_COORDINATE = 50.4501

        const val MINUTES_IN_DEGREE = 60
        const val SECONDS_IN_MINUTE = 60
    }
}
