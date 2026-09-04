package com.gps.zazor

import com.gps.zazor.data.models.Outing
import com.gps.zazor.data.models.Photo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class OutingTest {

    private val zone: ZoneId = ZoneId.of("Europe/Kyiv")

    private fun photo(
        day: Int, hour: Int, minute: Int = 0,
        lat: Double? = 50.4501, lng: Double? = 30.5234
    ) = Photo(
        path = "/p$day-$hour-$minute.jpg",
        name = "",
        date = LocalDateTime.of(2026, 9, day, hour, minute).atZone(zone).toInstant(),
        lat = lat,
        lng = lng
    )

    @Test
    fun `photos are grouped by the local calendar day`() {
        val outings = Outing.from(listOf(photo(3, 10), photo(3, 14), photo(4, 9)), zone)

        assertEquals(2, outings.size)
        assertEquals(LocalDate.of(2026, 9, 4), outings[0].date)
        assertEquals(2, outings[1].photos.size)
    }

    @Test
    fun `days run newest first and photos inside a day oldest first`() {
        val outings = Outing.from(listOf(photo(4, 9), photo(3, 14), photo(3, 10)), zone)

        assertEquals(LocalDate.of(2026, 9, 4), outings.first().date)
        val firstDay = outings.last()
        assertTrue(firstDay.photos[0].date.isBefore(firstDay.photos[1].date))
    }

    @Test
    fun `a shot just before local midnight belongs to that day, not the next`() {
        // 23:30 Kyiv on the 3rd is already the 4th in UTC: grouping on the wrong zone would split
        // an evening walk across two days.
        val outings = Outing.from(listOf(photo(3, 23, 30)), zone)

        assertEquals(LocalDate.of(2026, 9, 3), outings.single().date)
    }

    @Test
    fun `duration spans the first and last shot`() {
        val outing = Outing.from(listOf(photo(3, 10), photo(3, 11, 30)), zone).single()

        assertEquals(90 * 60L, outing.durationSeconds)
    }

    @Test
    fun `a single photo has no duration and no distance`() {
        val outing = Outing.from(listOf(photo(3, 10)), zone).single()

        assertEquals(0L, outing.durationSeconds)
        assertEquals(0.0, outing.distanceMeters, 0.001)
    }

    @Test
    fun `distance sums the legs between consecutive shots`() {
        val outing = Outing.from(
            listOf(
                photo(3, 10, lat = 50.4501, lng = 30.5234),
                photo(3, 11, lat = 50.4601, lng = 30.5234)
            ),
            zone
        ).single()

        // One hundredth of a degree of latitude is about 1112 m anywhere on Earth.
        assertEquals(1112.0, outing.distanceMeters, 5.0)
    }

    @Test
    fun `photos without a fix do not count as points and do not break the distance`() {
        val outing = Outing.from(
            listOf(
                photo(3, 10, lat = 50.4501, lng = 30.5234),
                photo(3, 10, 30, lat = null, lng = null),
                photo(3, 11, lat = 50.4601, lng = 30.5234)
            ),
            zone
        ).single()

        assertEquals(3, outing.photos.size)
        assertEquals(2, outing.pointCount)
        assertEquals(1112.0, outing.distanceMeters, 5.0)
    }

    @Test
    fun `an empty gallery produces no outings`() {
        assertTrue(Outing.from(emptyList(), zone).isEmpty())
    }

    @Test
    fun `the known distance between Kyiv and Lviv comes out right`() {
        val meters = Outing.distanceBetween(50.4501, 30.5234, 49.8397, 24.0297)

        assertEquals(468_000.0, meters, 5_000.0)
    }
}
