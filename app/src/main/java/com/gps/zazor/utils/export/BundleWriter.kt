package com.gps.zazor.utils.export

import android.content.Context
import com.gps.zazor.data.models.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packs photos, their voice notes, a text sheet and a track into one archive.
 *
 * A zip travels through every messenger intact, needs no server and no account, and the recipient
 * opens it with what they already have. It is the honest default for "send this to someone" - the
 * expiring link is the extra, not the baseline.
 */
class BundleWriter(private val context: Context) {

    companion object {

        const val DIR_BUNDLES = "bundles"
        const val MIME_ZIP = "application/zip"
        private const val BUFFER = 8 * 1024
    }

    private val directory: File
        get() = (context.getExternalFilesDir(DIR_BUNDLES) ?: File(context.filesDir, DIR_BUNDLES))
            .also { if (!it.exists()) it.mkdirs() }

    /**
     * @param includeTrack also writes a GPX alongside the photos.
     * @return the archive, or null when there was nothing to pack or the write failed.
     */
    suspend fun write(
        photos: List<Photo>,
        bundleName: String,
        report: String?,
        includeTrack: Boolean
    ): File? = withContext(Dispatchers.IO) {
        val existing = photos.filter { File(it.path).exists() }
        if (existing.isEmpty()) return@withContext null

        val target = File(directory, bundleName.toFileName() + ".zip")
        try {
            ZipOutputStream(target.outputStream().buffered()).use { zip ->
                existing.forEachIndexed { index, photo ->
                    val source = File(photo.path)
                    zip.putNextEntry(ZipEntry("%02d_%s".format(index + 1, source.name)))
                    source.inputStream().buffered().use { it.copyTo(zip, BUFFER) }
                    zip.closeEntry()

                    photo.voiceNotePath?.let(::File)?.takeIf { it.exists() }?.let { note ->
                        zip.putNextEntry(ZipEntry("%02d_%s".format(index + 1, note.name)))
                        note.inputStream().buffered().use { it.copyTo(zip, BUFFER) }
                        zip.closeEntry()
                    }
                }
                report?.let {
                    zip.putNextEntry(ZipEntry("report.txt"))
                    zip.write(it.toByteArray())
                    zip.closeEntry()
                }
                if (includeTrack && existing.any { it.lat != null && it.lng != null }) {
                    zip.putNextEntry(ZipEntry("track.gpx"))
                    zip.write(TrackExporter.toGpx(existing, bundleName).toByteArray())
                    zip.closeEntry()
                }
            }
            target
        } catch (e: Exception) {
            // A half-written archive is worse than none: the recipient would get a corrupt file.
            target.delete()
            null
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            directory.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            // Nothing to clean.
        }
        Unit
    }

    private fun String.toFileName(): String =
        replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").trim('_').take(60).ifEmpty { "zazor" }
}
