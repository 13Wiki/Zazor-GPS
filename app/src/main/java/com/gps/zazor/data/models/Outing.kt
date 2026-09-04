package com.gps.zazor.data.models

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One day's worth of shooting: where the person walked and what they photographed.
 *
 * Nothing here is stored. An outing is derived from the photos themselves, because every photo row
 * already carries a position and a timestamp. That is what makes "delete the photos and the trace
 * of that day goes with them" true by construction rather than by a second cleanup that can be
 * forgotten.
 */
data class Outing(
    val date: LocalDate,
    /** Oldest first, so the list reads in walking order. */
    val photos: List<Photo>
) {

    val pointCount: Int get() = photos.count { it.lat != null && it.lng != null }

    val startedAt: Instant? get() = photos.firstOrNull()?.date

    val finishedAt: Instant? get() = photos.lastOrNull()?.date

    /** Seconds between the first and last shot, or 0 for a single one. */
    val durationSeconds: Long
        get() {
            val from = startedAt ?: return 0
            val to = finishedAt ?: return 0
            return (to.epochSecond - from.epochSecond).coerceAtLeast(0)
        }

    /** Walked distance in metres, summed between consecutive located shots. */
    val distanceMeters: Double
        get() = photos
            .filter { it.lat != null && it.lng != null }
            .zipWithNext { a, b -> distanceBetween(a.lat!!, a.lng!!, b.lat!!, b.lng!!) }
            .sum()

    /** The photo whose fix the receiver should trust most - see [Photo] accuracy handling. */
    val cover: Photo? get() = photos.firstOrNull { it.lat != null && it.lng != null } ?: photos.firstOrNull()

    companion object {

        private const val EARTH_RADIUS_M = 6_371_000.0

        /**
         * Groups photos into outings by local calendar day, newest day first, each day's photos
         * oldest first.
         *
         * @param zone the zone the days are cut on; taken from the device so a day means the day
         *        the person actually had.
         */
        fun from(photos: List<Photo>, zone: ZoneId = ZoneId.systemDefault()): List<Outing> =
            photos
                .groupBy { LocalDate.ofInstant(it.date, zone) }
                .map { (date, dayPhotos) -> Outing(date, dayPhotos.sortedBy { it.date }) }
                .sortedByDescending { it.date }

        /** Great-circle distance. Accurate enough at the scale a person walks. */
        fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
            return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
