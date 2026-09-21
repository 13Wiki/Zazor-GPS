package com.gps.zazor.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gps.zazor.data.storage.dao.PhotosDao
import com.gps.zazor.data.storage.models.PhotoDb

@Database(entities = [PhotoDb::class], version = 5, exportSchema = false)
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
         * Adds the fix radius and the series grouping. Both are nullable, so existing rows keep
         * working: a photo taken before this build simply has no recorded accuracy.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photos ADD COLUMN accuracy_m REAL")
                db.execSQL("ALTER TABLE photos ADD COLUMN series_id TEXT")
            }
        }

        /**
         * Marks the frames taken on the wide lens, so the gallery can show those on their own.
         *
         * Not null with a default of 0: the app cannot tell in hindsight which lens an old photo
         * came from, and guessing would put frames under a filter they do not belong to.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photos ADD COLUMN is_wide INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Records which way the camera was pointing.
         *
         * Nullable rather than a default: a photo taken before this existed had no direction
         * recorded, and zero degrees means north, which would be a lie about every old frame.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photos ADD COLUMN bearing_deg REAL")
            }
        }

        /**
         * Built through DI instead of a global `lateinit` singleton, so nothing can reach the
         * database before it exists.
         */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
