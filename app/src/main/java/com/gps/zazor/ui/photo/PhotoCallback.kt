package com.gps.zazor.ui.photo

interface PhotoCallback {

    fun onCaptured()

    fun onPhotoEditCancel()

    fun clearAll()

    fun onCollageShown()

    fun onPhotoShown()

    fun onPanoramaShown()

    fun openSettings()

    fun openCollagePhoto(index: Int)

    fun switchEnabledCapture(isEnabled: Boolean)

    fun collapseEditPhoto()
}