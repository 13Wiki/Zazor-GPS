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

    @Delete
    suspend fun delete(photo: PhotoDb)

    // Suspend so it can never run a query on the main thread, which Room aborts with an exception.
    @Query("DELETE FROM photos")
    suspend fun clear()
}
