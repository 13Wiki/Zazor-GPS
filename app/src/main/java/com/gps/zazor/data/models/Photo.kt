package com.gps.zazor.data.models

import android.location.Location
import com.gps.zazor.data.storage.models.PhotoDb
import java.time.Instant

data class Photo(val path: String,
                 /** What the person called it - the note they typed, empty when they typed none. */
                 val name: String,
                 val date: Instant,
                 val address: String? = null,
                 val lat: Double? = null,
                 val lng: Double? = null,
                 /** Absolute path of the recorded voice note, or null when there is none. */
                 val voiceNotePath: String? = null,
                 /** Fix radius in metres at the moment of the shot. */
                 val accuracyMeters: Float? = null,
                 /** Set on every frame of one approach series. */
                 val seriesId: String? = null,
                 /** Taken on the wide lens; the gallery can show those on their own. */
                 val isWide: Boolean = false)

val Photo.location get() = Location("").apply {
    latitude = lat ?: 0.0
    longitude = lng ?: 0.0
}

val Photo.time get() = date.toEpochMilli()

/** True when the photo has a fix but no address yet - the offline case worth retrying. */
val Photo.needsAddress get() = address.isNullOrBlank() && lat != null && lng != null

fun Photo.toDb(): PhotoDb =
      PhotoDb(
          path,
          name,
          date.toEpochMilli(),
          address.orEmpty(),
          lat ?: 0.0,
          lng ?: 0.0,
          voiceNotePath,
          accuracyMeters,
          seriesId,
          isWide
      )
