package com.gps.zazor.data.models

import android.location.Location
import com.gps.zazor.data.storage.models.PhotoDb
import org.joda.time.DateTime

data class Photo(val path: String,
                 val name: String,
                 val date: DateTime,
                 val address: String? = null,
                 val lat: Double? = null,
                 val lng: Double? = null)

val Photo.location get() = Location("").apply {
    latitude = lat ?: 0.0
    longitude = lng ?: 0.0
}

val Photo.time get() = date.millis

fun Photo.toDb(): PhotoDb =
      PhotoDb(path, name, date.millis, address.orEmpty(), lat ?: 0.0, lng ?: 0.0)