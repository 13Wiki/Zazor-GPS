package com.gps.zazor

import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.models.toDb
import com.gps.zazor.data.storage.models.PhotoDb
import com.gps.zazor.data.storage.models.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * The database row stores epoch milliseconds and uses 0.0 as the "no coordinates" sentinel, so the
 * mapping in both directions is what decides whether a photo without a GPS fix renders its stamp
 * without coordinate lines instead of claiming to have been taken at Null Island.
 */
class PhotoMappingTest {

    private val takenAt: Instant = Instant.ofEpochMilli(1_772_000_000_000L)

    @Test
    fun `a photo survives a round trip through the database row`() {
        val photo = Photo("/photos/1.jpg", "", takenAt, "Тверская 1, Москва, RU", 55.7558, 37.6173)

        val restored = photo.toDb().toDomain()

        assertEquals(photo.path, restored.path)
        assertEquals(photo.date, restored.date)
        assertEquals(photo.address, restored.address)
        assertEquals(photo.lat!!, restored.lat!!, 1e-9)
        assertEquals(photo.lng!!, restored.lng!!, 1e-9)
    }

    @Test
    fun `absent coordinates stay absent rather than becoming zero`() {
        val photo = Photo("/photos/2.jpg", "", takenAt, address = null, lat = null, lng = null)

        val restored = photo.toDb().toDomain()

        assertNull(restored.lat)
        assertNull(restored.lng)
    }

    @Test
    fun `a null address is stored as empty text`() {
        assertEquals("", Photo("/photos/3.jpg", "", takenAt).toDb().address)
    }

    @Test
    fun `the stored timestamp is epoch milliseconds`() {
        assertEquals(takenAt.toEpochMilli(), Photo("/p.jpg", "", takenAt).toDb().date)
    }

    @Test
    fun `a row read back from an older build still decodes`() {
        val row = PhotoDb("/photos/4.jpg", "", takenAt.toEpochMilli(), "", 0.0, 0.0)

        val photo = row.toDomain()

        assertEquals(takenAt, photo.date)
        assertNull(photo.lat)
        assertNull(photo.lng)
    }
}
