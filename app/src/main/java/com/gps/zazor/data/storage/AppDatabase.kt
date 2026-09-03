package com.gps.zazor.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gps.zazor.data.storage.dao.PhotosDao
import com.gps.zazor.data.storage.models.PhotoDb

@Database(entities = [PhotoDb::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photosDao(): PhotosDao

    companion object {

        private const val DATABASE_NAME = "zazorPhotos.db"

        /**
         * Built through DI instead of a global `lateinit` singleton, so nothing can reach the
         * database before it exists.
         */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
