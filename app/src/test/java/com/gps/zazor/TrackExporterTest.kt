package com.gps.zazor

import com.gps.zazor.data.models.Photo
import com.gps.zazor.utils.export.TrackExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TrackExporterTest {

    private fun photo(
        path: String,
        seconds: Long,
        lat: Double? = 55.755826,
        lng: Double? = 37.617300,
        address: String? = null,
        name: String = ""
    ) = Photo(path, name, Instant.ofEpochSecond(seconds), address, lat, lng)

    private val first = photo("/a.jpg", 1_772_000_000, 55.755826, 37.617300, "Тверская 1")
    private val second = photo("/b.jpg", 1_772_000_600, 55.756900, 37.618400, "Тверская 5")

    @Test
    fun `gpx carries one waypoint per located photo`() {
        val gpx = TrackExporter.toGpx(listOf(first, second), "Выход")

        assertEquals(2, Regex("<wpt ").findAll(gpx).count())
        assertTrue(gpx.contains("""lat="55.755826""""))
        assertTrue(gpx.contains("""lon="37.617300""""))
    }

    @Test
    fun `gpx timestamps are UTC in ISO-8601`() {
        val gpx = TrackExporter.toGpx(listOf(first), "Выход")

        // A navigator rejects a local-time stamp without a zone.
        assertTrue(gpx.contains("<time>2026-02-25T06:13:20Z</time>"))
    }

    @Test
    fun `points are ordered oldest first regardless of input order`() {
        val gpx = TrackExporter.toGpx(listOf(second, first), "Выход")

        assertTrue(gpx.indexOf("55.755826") < gpx.indexOf("55.756900"))
    }

    @Test
    fun `photos without a fix are skipped rather than written as zero`() {
        val noFix = photo("/c.jpg", 1_772_000_900, lat = null, lng = null)

        val gpx = TrackExporter.toGpx(listOf(first, noFix), "Выход")

        assertEquals(1, Regex("<wpt ").findAll(gpx).count())
        assertFalse(gpx.contains("0.000000"))
    }

    @Test
    fun `a lone point produces no track segment`() {
        val gpx = TrackExporter.toGpx(listOf(first), "Выход")

        assertFalse(gpx.contains("<trkseg>"))
    }

    @Test
    fun `two or more points produce a track segment`() {
        val gpx = TrackExporter.toGpx(listOf(first, second), "Выход")

        assertEquals(2, Regex("<trkpt ").findAll(gpx).count())
    }

    @Test
    fun `ampersands and angle brackets in a note are escaped`() {
        val risky = photo("/d.jpg", 1_772_000_000, address = """Дом 5 & 6 <под "аркой">""")

        val gpx = TrackExporter.toGpx(listOf(risky), "Выход")

        assertTrue(gpx.contains("&amp;"))
        assertTrue(gpx.contains("&lt;"))
        assertTrue(gpx.contains("&quot;"))
        assertFalse(gpx.contains("""Дом 5 & 6"""))
    }

    @Test
    fun `an empty list still produces a parseable gpx`() {
        val gpx = TrackExporter.toGpx(emptyList(), "Пусто")

        assertTrue(gpx.startsWith("<?xml"))
        assertTrue(gpx.trimEnd().endsWith("</gpx>"))
        assertFalse(gpx.contains("<wpt "))
    }

    @Test
    fun `kml puts longitude before latitude`() {
        val kml = TrackExporter.toKml(listOf(first), "Выход")

        // KML is lon,lat - the reverse of GPX, and the classic way to land on the wrong continent.
        assertTrue(kml.contains("<coordinates>37.617300,55.755826,0</coordinates>"))
    }

    @Test
    fun `kml draws a line only when there is more than one point`() {
        assertFalse(TrackExporter.toKml(listOf(first), "Выход").contains("<LineString>"))
        assertTrue(TrackExporter.toKml(listOf(first, second), "Выход").contains("<LineString>"))
    }

    @Test
    fun `kml escapes the track name`() {
        val kml = TrackExporter.toKml(listOf(first), """Лес & поле""")

        assertTrue(kml.contains("Лес &amp; поле"))
    }
}
