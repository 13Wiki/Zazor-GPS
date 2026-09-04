package com.gps.zazor.utils.export

import com.gps.zazor.data.models.Photo
import java.time.format.DateTimeFormatter

/**
 * Turns a day's photos into a GPX or KML track that any navigator can open.
 *
 * Pure string building with no Android dependencies, so it is covered by unit tests rather than
 * only by eye. Only photos that actually carry a fix take part - a shot saved without GPS has no
 * place on a map and is skipped rather than written out as 0,0.
 */
object TrackExporter {

    /** GPX and KML both require UTC in ISO-8601, not local time. */
    private val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    /** Coordinates are written with 6 decimals - about 0.1 m, finer than any phone fix. */
    private const val COORD_FORMAT = "%.6f"

    fun toGpx(photos: List<Photo>, trackName: String): String {
        val located = photos.located()
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
            append(
                """<gpx version="1.1" creator="Zazor" xmlns="http://www.topografix.com/GPX/1/1">"""
            ).append('\n')
            append("  <metadata><name>").append(trackName.escapeXml()).append("</name></metadata>\n")

            located.forEach { photo ->
                append("  <wpt lat=\"").append(photo.lat.format())
                    .append("\" lon=\"").append(photo.lng.format()).append("\">\n")
                append("    <time>").append(TIMESTAMP.format(photo.date)).append("</time>\n")
                append("    <name>").append(photo.waypointName().escapeXml()).append("</name>\n")
                photo.description()?.let {
                    append("    <desc>").append(it.escapeXml()).append("</desc>\n")
                }
                append("  </wpt>\n")
            }

            // A single point is a waypoint, not a route: a one-point <trkseg> is meaningless.
            if (located.size > 1) {
                append("  <trk>\n    <name>").append(trackName.escapeXml()).append("</name>\n")
                append("    <trkseg>\n")
                located.forEach { photo ->
                    append("      <trkpt lat=\"").append(photo.lat.format())
                        .append("\" lon=\"").append(photo.lng.format()).append("\">")
                    append("<time>").append(TIMESTAMP.format(photo.date)).append("</time>")
                    append("</trkpt>\n")
                }
                append("    </trkseg>\n  </trk>\n")
            }
            append("</gpx>\n")
        }
    }

    fun toKml(photos: List<Photo>, trackName: String): String {
        val located = photos.located()
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
            append("""<kml xmlns="http://www.opengis.net/kml/2.2">""").append('\n')
            append("  <Document>\n    <name>").append(trackName.escapeXml()).append("</name>\n")

            located.forEach { photo ->
                append("    <Placemark>\n")
                append("      <name>").append(photo.waypointName().escapeXml()).append("</name>\n")
                photo.description()?.let {
                    append("      <description>").append(it.escapeXml()).append("</description>\n")
                }
                append("      <TimeStamp><when>").append(TIMESTAMP.format(photo.date))
                    .append("</when></TimeStamp>\n")
                // KML orders coordinates longitude first - the reverse of GPX.
                append("      <Point><coordinates>").append(photo.lng.format())
                    .append(",").append(photo.lat.format())
                    .append(",0</coordinates></Point>\n")
                append("    </Placemark>\n")
            }

            if (located.size > 1) {
                append("    <Placemark>\n      <name>").append(trackName.escapeXml())
                    .append("</name>\n")
                append("      <LineString><tessellate>1</tessellate><coordinates>\n")
                located.forEach { photo ->
                    append("        ").append(photo.lng.format()).append(",")
                        .append(photo.lat.format()).append(",0\n")
                }
                append("      </coordinates></LineString>\n    </Placemark>\n")
            }
            append("  </Document>\n</kml>\n")
        }
    }

    /** Photos with a fix, oldest first, so the track runs in walking order. */
    private fun List<Photo>.located(): List<Photo> =
        filter { it.lat != null && it.lng != null }.sortedBy { it.date }

    private fun Photo.waypointName(): String =
        name.takeIf { it.isNotBlank() }
            ?: address?.takeIf { it.isNotBlank() }
            ?: TIMESTAMP.format(date)

    private fun Photo.description(): String? =
        address?.takeIf { it.isNotBlank() && it != waypointName() }

    private fun Double?.format(): String = String.format(java.util.Locale.US, COORD_FORMAT, this)

    /**
     * A note or an address can legitimately contain `&`, `<` or a quote; unescaped they produce a
     * file no navigator will parse.
     */
    private fun String.escapeXml(): String = buildString(length) {
        this@escapeXml.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }
}
