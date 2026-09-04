package com.gps.zazor

import com.gps.zazor.utils.export.TrackFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackFormatTest {

    @Test
    fun `formats carry the extension and mime type a receiving app expects`() {
        assertEquals(".gpx", TrackFormat.GPX.extension)
        assertEquals("application/gpx+xml", TrackFormat.GPX.mimeType)
        assertEquals(".kml", TrackFormat.KML.extension)
        assertEquals("application/vnd.google-earth.kml+xml", TrackFormat.KML.mimeType)
    }
}
