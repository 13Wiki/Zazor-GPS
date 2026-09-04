package com.gps.zazor.ui.photo.panorama

import android.graphics.Bitmap
import com.gps.zazor.R
import com.gps.zazor.ui.photo.base.BasePhotoContract
import com.gps.zazor.ui.photo.base.BasePhotoFragment

/**
 * Wide-angle capture, on the phone's own ultra-wide lens.
 *
 * This replaces the stitched panorama that used to run on a proprietary Camera1 SDK from 2017.
 * A modern phone's widest back lens covers roughly 120 degrees in a single frame, which is
 * instant, cannot fail to stitch, adds nothing to the download, and - the part that actually
 * mattered - goes through exactly the same pipeline as any other photo. The old panorama saved
 * its file straight to the gallery, so it carried no coordinate stamp and nothing could be marked
 * on it; here the stamp, the marks, the note and the metadata scrub all come for free.
 *
 * On a device with only one back camera the tab still works - it simply shoots on the normal lens.
 */
class PanoramaFragment : BasePhotoFragment() {

    override val screenTitle = R.string.panorama

    /** Binds the widest back lens available, falling back to the normal one. */
    override val useUltraWide = true

    override fun onPhotoReady(bitmap: Bitmap) {
        viewModel.sendEvent(BasePhotoContract.Event.SaveEdits(bitmap))
    }
}
