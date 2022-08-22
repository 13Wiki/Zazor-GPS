package com.gps.zazor.data.storage.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gps.zazor.data.models.Photo
import org.joda.time.DateTime


@Entity(tableName = "photos")
data class PhotoDb(@PrimaryKey val path: String,
                   val name: String,
                   val date: Long,
                   val address: String,
                   val lat: Double,
                   val lng: Double)

fun PhotoDb.toDomain(): Photo =
      Photo(path, name, DateTime(date), address, lat.takeUnless { it == 0.0 }, lng.takeUnless { it == 0.0 })