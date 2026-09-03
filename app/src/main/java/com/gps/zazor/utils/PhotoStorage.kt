package com.gps.zazor.utils

import android.content.Context
import android.graphics.Bitmap
import com.gps.zazor.utils.extensions.toBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes captured photos into the app-private external directory.
 *
 * Everything here runs on [Dispatchers.IO] - the old code compressed bitmaps and wrote files on
 * whatever thread the coroutine happened to start on, which for `viewModelScope.launch {}` is the
 * main thread.
 */
class PhotoStorage(private val context: Context) {

    companion object {

        const val DIR_PHOTOS = "photos"
        private const val EXTENSION = ".jpg"
    }

    private val directory: File
        get() = (context.getExternalFilesDir(DIR_PHOTOS) ?: File(context.filesDir, DIR_PHOTOS))
            .also { if (!it.exists()) it.mkdirs() }

    /**
     * @param path absolute path of an existing photo to overwrite, or `null` to create a new file.
     * @return absolute path of the stored file, or `null` when it could not be written.
     */
    suspend fun save(bitmap: Bitmap, path: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val file = path?.let(::File) ?: File(directory, System.currentTimeMillis().toString() + EXTENSION)
            file.outputStream().buffered().use { it.write(bitmap.toBytes()) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(path).takeIf { it.exists() }?.delete() ?: true
        } catch (e: Exception) {
            false
        }
    }
}
