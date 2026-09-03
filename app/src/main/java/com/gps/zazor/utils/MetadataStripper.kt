package com.gps.zazor.utils

import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Removes every EXIF tag from a JPEG on disk.
 *
 * The camera, collage and re-edit paths never produce EXIF in the first place: they decode the
 * capture into a [android.graphics.Bitmap] and re-encode it with `Bitmap.compress`, which writes
 * no metadata segment at all. The panorama path is different - the Dermandar SDK stitches and
 * saves the file itself and calls `DMDExif.copyExif`, which copies Make, Model, Software,
 * DateTimeOriginal and the whole GPS block from the source frames into the result.
 *
 * The point of this app is that the position is *drawn on the picture*, never carried in its
 * metadata, so anything the app did not encode itself is scrubbed before it reaches the gallery.
 */
object MetadataStripper {

    /**
     * Tags cleared explicitly. [ExifInterface] only rewrites attributes it knows, so listing the
     * identifying ones guarantees they are gone even when a tag is not part of a newer standard.
     */
    private val TAGS = arrayOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_MAKER_NOTE,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_XMP
    )

    /** @return true when the file carries no identifying metadata afterwards. */
    suspend fun strip(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || !file.canWrite()) return@withContext false
        try {
            ExifInterface(file).run {
                TAGS.forEach { setAttribute(it, null) }
                saveAttributes()
            }
            !hasIdentifyingMetadata(path)
        } catch (e: Exception) {
            false
        }
    }

    /** Reads the file back and reports whether anything identifying survived. */
    fun hasIdentifyingMetadata(path: String): Boolean =
        try {
            ExifInterface(path).run {
                getLatLong() != null ||
                    !getAttribute(ExifInterface.TAG_MAKE).isNullOrEmpty() ||
                    !getAttribute(ExifInterface.TAG_MODEL).isNullOrEmpty() ||
                    !getAttribute(ExifInterface.TAG_SOFTWARE).isNullOrEmpty()
            }
        } catch (e: Exception) {
            false
        }
}
