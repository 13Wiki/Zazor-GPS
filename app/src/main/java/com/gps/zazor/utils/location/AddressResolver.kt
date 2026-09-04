package com.gps.zazor.utils.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Reverse-geocodes a position into a human readable address.
 *
 * [Geocoder.getFromLocation] performs a network call and throws [java.io.IOException] when the
 * device is offline or the backend is unreachable. The old code called it unguarded while saving
 * a photo, so taking a picture without connectivity crashed the app. Now an unreachable geocoder
 * simply yields `null`, the photo is stored without an address, and
 * [com.gps.zazor.data.repositories.PhotoRepository.backfillAddresses] fills it in later.
 */
class AddressResolver(private val context: Context) {

    private val geocoder by lazy { Geocoder(context, Locale.getDefault()) }

    suspend fun resolve(location: Location?): String? =
        location?.let { resolve(it.latitude, it.longitude) }

    suspend fun resolve(latitude: Double?, longitude: Double?): String? {
        if (latitude == null || longitude == null) return null
        if (!Geocoder.isPresent()) return null
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull()
                    ?.format()
                    ?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun Address.format(): String =
        listOfNotNull(
            getAddressLine(0),
            locality.takeUnless { it.isNullOrBlank() || getAddressLine(0)?.contains(it) == true },
            countryCode.takeUnless { it.isNullOrBlank() }
        ).joinToString(", ")
}
