package com.gps.zazor.data.storage

import android.content.Context
import androidx.room.Room

object AppDatabaseCreator {

    private const val DATABASE_NAME = "zazorPhotos.db"

    lateinit var database: AppDatabase

    fun createDatabase(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()
    }
}