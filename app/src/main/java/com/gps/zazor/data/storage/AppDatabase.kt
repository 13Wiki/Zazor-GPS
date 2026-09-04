package com.gps.zazor.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gps.zazor.data.storage.dao.PhotosDao
import com.gps.zazor.data.storage.models.PhotoDb

@Database(entities = [PhotoDb::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photosDao(): PhotosDao

    companion object {

        private const val DATABASE_NAME = "zazorPhotos.db"

        /**
         * Adds the voice-note column. A real migration rather than a destructive fallback: an
         * existing install must not lose its gallery to an app update.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photos ADD COLUMN voice_note_path TEXT")
            }
        }

        /**
         * Built through DI instead of a global `lateinit` singleton, so nothing can reach the
         * database before it exists.
         */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
