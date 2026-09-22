package com.gps.zazor.ui.photo

interface PhotoCallback {

    /** @param isWide a panorama, which reviews itself on its own screen rather than in a sheet. */
    fun onCaptured(isWide: Boolean)

    fun onPhotoEditCancel()

    fun clearAll()

    /** Takes back the last drawn mark; what the corner button does while drawing. */
    fun undoPaint()

    fun onCollageShown()

    fun onPhotoShown()

    fun onPanoramaShown()

    fun openSettings()

    fun openCollagePhoto(index: Int)

    fun switchEnabledCapture(isEnabled: Boolean)

    fun collapseEditPhoto()
}