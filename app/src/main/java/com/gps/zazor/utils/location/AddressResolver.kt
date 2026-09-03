package com.gps.zazor.utils.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import java.util.Locale

/**
 * Reverse-geocodes a position into a human readable address.
 *
 * [Geocoder.getFromLocation] performs a network call and throws [java.io.IOException] when the
 * device is offline or the backend is unreachable. The old code called it unguarded while saving
 * a photo, so taking a picture without connectivity crashed the app.
 */
class AddressResolver(private val context: Context) {

    private val geocoder by lazy { Geocoder(context, Locale.getDefault()) }

    fun resolve(location: Location?): String? {
        location ?: return null
        if (!Geocoder.isPresent()) return null
        return try {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.format()
        } catch (e: Exception) {
            null
        }
    }

    private fun Address.format(): String =
        listOfNotNull(
            getAddressLine(0),
            locality.takeUnless { it.isNullOrBlank() || getAddressLine(0)?.contains(it) == true },
            countryCode.takeUnless { it.isNullOrBlank() }
        ).joinToString(", ")
}
