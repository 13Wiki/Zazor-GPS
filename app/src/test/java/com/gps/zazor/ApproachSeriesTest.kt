package com.gps.zazor

import com.gps.zazor.data.models.ApproachSeries
import com.gps.zazor.data.models.Photo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * The series exists to be walked into, so the rule that matters is which frame's coordinate it
 * reports: the tightest fix, not the last frame. The last frame is usually the closest to the
 * object and, being under cover, often the worst fix of the set.
 */
class ApproachSeriesTest {

    private fun frame(
        second: Long,
        accuracy: Float?,
        lat: Double? = 50.4501,
        lng: Double? = 30.5234,
        series: String? = "s1"
    ) = Photo(
        path = "/f$second.jpg",
        name = "",
        date = Instant.ofEpochSecond(1_772_000_000 + second),
        lat = lat,
        lng = lng,
        accuracyMeters = accuracy,
        seriesId = series
    )

    @Test
    fun `frames of one series are grouped, oldest first`() {
        val series = ApproachSeries.from(
            listOf(frame(30, 7F), frame(0, 12F), frame(60, 3F))
        ).single()

        assertEquals(3, series.size)
        assertEquals(12F, series.frames.first().accuracyMeters)
        assertEquals(3F, series.frames.last().accuracyMeters)
    }

    @Test
    fun `the series coordinate comes from the tightest fix, not the last frame`() {
        val series = ApproachSeries.from(
            listOf(
                frame(0, 12F, lat = 50.0),
                frame(30, 3F, lat = 51.0),   // best fix, mid-walk
                frame(60, 18F, lat = 52.0)   // closest to the object, worst fix
            )
        ).single()

        assertEquals(3F, series.accuracyMeters)
        assertEquals(51.0, series.lat!!, 1e-9)
    }

    @Test
    fun `the closest frame is still the one shown as the thumbnail`() {
        val series = ApproachSeries.from(
            listOf(frame(0, 12F), frame(60, 18F))
        ).single()

        assertEquals(18F, series.closest?.accuracyMeters)
    }

    @Test
    fun `frames without a fix never win`() {
        val series = ApproachSeries.from(
            listOf(frame(0, null, lat = null, lng = null), frame(30, 9F, lat = 51.0))
        ).single()

        assertEquals(51.0, series.lat!!, 1e-9)
    }

    @Test
    fun `a located frame with no recorded accuracy is used when nothing better exists`() {
        val series = ApproachSeries.from(listOf(frame(0, null, lat = 51.0))).single()

        assertEquals(51.0, series.lat!!, 1e-9)
        assertNull(series.accuracyMeters)
    }

    @Test
    fun `a series with no fixes at all reports none`() {
        val series = ApproachSeries.from(
            listOf(frame(0, null, lat = null, lng = null))
        ).single()

        assertNull(series.lat)
        assertNull(series.bestFix)
    }

    @Test
    fun `standalone photos are not series`() {
        assertTrue(ApproachSeries.from(listOf(frame(0, 5F, series = null))).isEmpty())
    }

    @Test
    fun `two series stay apart and the most recent comes first`() {
        val series = ApproachSeries.from(
            listOf(frame(0, 5F, series = "old"), frame(100, 5F, series = "new"))
        )

        assertEquals(2, series.size)
        assertEquals("new", series.first().id)
    }
}
