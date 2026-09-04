package com.gps.zazor.data.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gps.zazor.data.storage.models.PhotoDb

@Dao
interface PhotosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePhoto(photo: PhotoDb)

    @Query("SELECT * FROM photos ORDER BY date DESC")
    suspend fun getAll(): List<PhotoDb>

    @Query("SELECT * FROM photos WHERE path = :path LIMIT 1")
    suspend fun getPhoto(path: String): PhotoDb?

    @Query("SELECT * FROM photos ORDER BY date DESC LIMIT 1")
    suspend fun getLast(): PhotoDb?

    /** Rows saved with a fix but no address - the ones taken while offline. */
    @Query("SELECT * FROM photos WHERE (address IS NULL OR address = '') AND NOT (lat = 0.0 AND lng = 0.0)")
    suspend fun getWithoutAddress(): List<PhotoDb>

    @Query("UPDATE photos SET address = :address WHERE path = :path")
    suspend fun updateAddress(path: String, address: String)

    @Query("UPDATE photos SET voice_note_path = :voiceNotePath WHERE path = :path")
    suspend fun updateVoiceNote(path: String, voiceNotePath: String?)

    @Delete
    suspend fun delete(photo: PhotoDb)

    // Suspend so it can never run a query on the main thread, which Room aborts with an exception.
    @Query("DELETE FROM photos")
    suspend fun clear()
}
