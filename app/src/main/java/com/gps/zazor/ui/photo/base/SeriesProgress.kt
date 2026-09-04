package com.gps.zazor.ui.photo.base

/**
 * State of the open approach series.
 *
 * Kept in its own flow rather than in the screen's UiState: saving a frame emits both "hide the
 * preview" and "the series grew", and a conflated StateFlow would drop one of them - leaving
 * either the counter stale or the preview stuck on screen.
 *
 * @param isOpen distinguishes a freshly opened series, which has no frames yet, from no series.
 * @param bestAccuracy tightest fix among the frames; this is the coordinate the series reports.
 */
data class SeriesProgress(
    val isOpen: Boolean = false,
    val frameCount: Int = 0,
    val bestAccuracy: Float? = null
)
