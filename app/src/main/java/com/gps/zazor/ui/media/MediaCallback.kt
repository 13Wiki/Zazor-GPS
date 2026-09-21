package com.gps.zazor.ui.media

interface MediaCallback {

    fun editPhoto(photoPath: String)

    /** Opens the outings log: the days walked, with each day's track. */
    fun openOutings()

    /** Opens one day's track full screen. @param epochDay the day, as days since the epoch. */
    fun openOutingMap(epochDay: Long)

    /** Opens the transfer screen for the given photos. */
    fun openShare(paths: List<String>)

    /** Opens one approach series: its frames, and which of them the coordinate comes from. */
    fun openSeries(seriesId: String)

    /** Back to the camera with that series open, so the next shot joins it. */
    fun addToSeries(seriesId: String)
}
