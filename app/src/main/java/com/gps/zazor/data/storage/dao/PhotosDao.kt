package com.gps.zazor.data.storage.dao

import androidx.room.*
import com.gps.zazor.data.storage.models.PhotoDb

@Dao
interface PhotosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePhoto(photo: PhotoDb)

    @Query("SELECT * FROM photos")
    suspend fun getAll(): List<PhotoDb>

    @Query("SELECT * FROM photos WHERE path=:path")
    suspend fun getPhoto(path: String): PhotoDb?

    @Query("SELECT * FROM photos ORDER BY date DESC LIMIT 1")
    suspend fun getLast(): List<PhotoDb>

    @Delete
    suspend fun delete(photo: PhotoDb)

    @Query("DELETE FROM photos")
    fun clear()
}