package com.gps.zazor.data.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gps.zazor.data.storage.dao.PhotosDao
import com.gps.zazor.data.storage.models.PhotoDb

@Database(entities = [PhotoDb::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photosDao(): PhotosDao
}