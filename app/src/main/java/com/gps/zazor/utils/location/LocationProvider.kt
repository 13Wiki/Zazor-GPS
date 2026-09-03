package com.gps.zazor.utils.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

/**
 * Streams the device position while the collector is active.
 *
 * The previous implementation asked for `lastLocation` exactly once, in the ViewModel's `init` -
 * before the user had granted the location permission - so on a fresh install the location was
 * always `null` and, because the capture flow refused to build a note without one, the shutter
 * appeared to do nothing at all.
 */
class LocationProvider(private val context: Context) {

    companion object {

        private const val UPDATE_INTERVAL_MS = 5_000L
        private const val MIN_UPDATE_INTERVAL_MS = 2_000L
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Emits the last known position immediately (when there is one), then every fix that follows.
     * Emits nothing at all - rather than failing - when the permission has not been granted.
     */
    @SuppressLint("MissingPermission")
    fun locations(): Flow<Location> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        client.lastLocation.addOnSuccessListener { location -> location?.let { trySend(it) } }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .build()
        client.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
