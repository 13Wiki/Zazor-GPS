package com.gps.zazor.ui.media

interface MediaCallback {

    fun editPhoto(photoPath: String)

    /** Opens the outings log: the days walked, with each day's track. */
    fun openOutings()

    /** Opens the transfer screen for the given photos. */
    fun openShare(paths: List<String>)
}
