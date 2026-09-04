package com.gps.zazor.utils.export

import android.content.Context
import com.gps.zazor.R
import com.gps.zazor.data.models.Photo
import com.gps.zazor.utils.time.PhotoClock
import java.util.Locale

/**
 * The plain-text sheet that travels with a set of photos.
 *
 * Written as text rather than a document format on purpose: it pastes straight into a messenger,
 * survives any client, and needs no library. What it must never do is add information the photos
 * themselves do not carry - the coordinates here are the ones already burned onto the pictures.
 */
object ReportBuilder {

    fun build(context: Context, photos: List<Photo>, includeCoordinates: Boolean = true): String =
        buildString {
            appendLine(context.getString(R.string.report_title))
            appendLine(context.getString(R.string.report_count, photos.size))
            appendLine()
            photos.sortedBy { it.date }.forEachIndexed { index, photo ->
                appendLine("${index + 1}. ${PhotoClock.formatDateTime(photo.date)}")
                if (includeCoordinates && photo.lat != null && photo.lng != null) {
                    appendLine("   ${photo.lat.format()}, ${photo.lng.format()}")
                    photo.accuracyMeters?.let {
                        appendLine("   " + context.getString(R.string.report_accuracy, it.toInt()))
                    }
                }
                // An address is a location in words: unticking coordinates must drop it too.
                if (includeCoordinates) {
                    photo.address?.takeIf { it.isNotBlank() }?.let { appendLine("   $it") }
                }
                photo.name.takeIf { it.isNotBlank() }?.let { appendLine("   $it") }
                appendLine()
            }
            append(context.getString(R.string.report_footer))
        }

    private fun Double?.format(): String =
        this?.let { String.format(Locale.US, "%.6f", it) } ?: ""
}
