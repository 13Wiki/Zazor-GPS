package com.gps.zazor.utils.export

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.gps.zazor.BuildConfig
import com.gps.zazor.R
import java.io.File

/**
 * Hands a written track to whatever the person wants to send it with.
 *
 * Shared by the outings list and the map of one outing: both end at the same chooser, and the file
 * leaves through a content URI rather than a path, so no other app gets a look at the folder.
 */
fun Fragment.shareTrack(file: File, format: TrackFormat) {
    val uri = try {
        FileProvider.getUriForFile(
            requireContext(),
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
    } catch (e: IllegalArgumentException) {
        Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_SHORT).show()
        return
    }
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = format.mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            getString(R.string.share)
        )
    )
}
