package com.gps.zazor.data.storage.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gps.zazor.data.models.Photo
import java.time.Instant

/**
 * [date] stays epoch milliseconds, so swapping the date library needed no schema migration.
 * [voiceNotePath] arrived in schema 2, [accuracyMeters] and [seriesId] in schema 3; all three are
 * null for every row written before them.
 */
@Entity(tableName = "photos")
data class PhotoDb(@PrimaryKey val path: String,
                   val name: String,
                   val date: Long,
                   val address: String,
                   val lat: Double,
                   val lng: Double,
                   @ColumnInfo(name = "voice_note_path") val voiceNotePath: String? = null,
                   /** Fix radius in metres at the moment of the shot; null when there was none. */
                   @ColumnInfo(name = "accuracy_m") val accuracyMeters: Float? = null,
                   /** Groups the frames of one approach series; null for a standalone shot. */
                   @ColumnInfo(name = "series_id") val seriesId: String? = null)

fun PhotoDb.toDomain(): Photo =
      Photo(
          path,
          name,
          Instant.ofEpochMilli(date),
          address,
          lat.takeUnless { it == 0.0 },
          lng.takeUnless { it == 0.0 },
          voiceNotePath,
          accuracyMeters,
          seriesId
      )
