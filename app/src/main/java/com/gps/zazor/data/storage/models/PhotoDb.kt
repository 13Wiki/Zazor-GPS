package com.gps.zazor.data.storage.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gps.zazor.data.models.Photo
import java.time.Instant

/** [date] stays epoch milliseconds, so swapping the date library needed no schema migration. */
@Entity(tableName = "photos")
data class PhotoDb(@PrimaryKey val path: String,
                   val name: String,
                   val date: Long,
                   val address: String,
                   val lat: Double,
                   val lng: Double)

fun PhotoDb.toDomain(): Photo =
      Photo(
          path,
          name,
          Instant.ofEpochMilli(date),
          address,
          lat.takeUnless { it == 0.0 },
          lng.takeUnless { it == 0.0 }
      )
