package com.gps.zazor.ui.photo

interface PhotoCallback {

    fun onCaptured()

    fun onPhotoEditCancel()

    fun clearAll()

    /** Takes back the last drawn mark; what the corner button does while drawing. */
    fun undoPaint()

    /** Opens the editor on the marker tool, the way a panorama starts. */
    fun startMarkerMode()

    fun onCollageShown()

    fun onPhotoShown()

    fun onPanoramaShown()

    fun openSettings()

    fun openCollagePhoto(index: Int)

    fun switchEnabledCapture(isEnabled: Boolean)

    fun collapseEditPhoto()
}