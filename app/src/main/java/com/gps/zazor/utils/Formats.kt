package com.gps.zazor.utils

import android.content.Context
import com.gps.zazor.R
import java.util.Locale

/**
 * Human-readable distance and duration. Kept in one place so a route card, a series and an export
 * never disagree about how far "1,2 км" is.
 */
object Formats {

    private const val METERS_IN_KM = 1000

    fun distance(context: Context, meters: Double): String =
        if (meters < METERS_IN_KM) {
            context.getString(R.string.distance_m, meters.toInt())
        } else {
            context.getString(
                R.string.distance_km,
                String.format(Locale.getDefault(), "%.1f", meters / METERS_IN_KM)
            )
        }

    /**
     * A heading as it is read out loud: "обзор 214°". Rounded to a degree - the compass in a
     * phone is not worth a decimal, and half a degree is not a difference anyone can walk.
     */
    fun bearing(context: Context, degrees: Float): String =
        context.getString(R.string.bearing_view, Math.round(degrees) % 360)

    fun duration(context: Context, seconds: Long): String {
        val minutes = seconds / 60
        return if (minutes >= 60) {
            context.getString(R.string.duration_hm, minutes / 60, minutes % 60)
        } else {
            context.getString(R.string.duration_m, minutes)
        }
    }
}
