package com.gps.zazor.ui.photo.base

/**
 * What the stamp would say if the shutter were pressed right now.
 *
 * Shown over the viewfinder so nobody has to take a picture to find out what will be written on
 * it - and so an address that has not resolved yet, or a fix that is still drifting, is visible
 * before the shot rather than after.
 *
 * Every field is null when the setting that prints it is off, so the card and the stamp never
 * disagree about what ends up on the photograph.
 */
data class StampPreview(
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String? = null,
    val accuracyMeters: Float? = null
) {

    val hasPosition: Boolean get() = lat != null && lng != null
}
