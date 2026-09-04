package com.gps.zazor.utils.export

import android.content.Context
import com.gps.zazor.data.models.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class TrackFormat(val extension: String, val mimeType: String) {

    /** Opens in almost every navigator and hiking app. */
    GPX(".gpx", "application/gpx+xml"),

    /** Opens in Google Earth and Google My Maps. */
    KML(".kml", "application/vnd.google-earth.kml+xml")
}

/**
 * Writes an exported track into the app's own directory so it can be handed to another app
 * through the FileProvider.
 */
class TrackFileWriter(private val context: Context) {

    companion object {

        const val DIR_TRACKS = "tracks"
    }

    private val directory: File
        get() = (context.getExternalFilesDir(DIR_TRACKS) ?: File(context.filesDir, DIR_TRACKS))
            .also { if (!it.exists()) it.mkdirs() }

    /**
     * @return the written file, or null when there was nothing to write or the write failed.
     */
    suspend fun write(
        photos: List<Photo>,
        format: TrackFormat,
        trackName: String
    ): File? = withContext(Dispatchers.IO) {
        // Exporting a file with no points would hand the user an empty document.
        if (photos.none { it.lat != null && it.lng != null }) return@withContext null
        val content = when (format) {
            TrackFormat.GPX -> TrackExporter.toGpx(photos, trackName)
            TrackFormat.KML -> TrackExporter.toKml(photos, trackName)
        }
        try {
            File(directory, trackName.toFileName() + format.extension)
                .also { it.writeText(content) }
        } catch (e: Exception) {
            null
        }
    }

    /** Old exports are disposable; clearing them keeps the directory from growing forever. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            directory.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            // Nothing to clean.
        }
        Unit
    }

    /** Strips anything a file system would object to, and keeps the name short. */
    private fun String.toFileName(): String =
        replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").trim('_').take(60).ifEmpty { "track" }
}
