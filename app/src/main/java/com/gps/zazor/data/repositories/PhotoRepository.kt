package com.gps.zazor.data.repositories

import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.models.toDb
import com.gps.zazor.data.storage.dao.PhotosDao
import com.gps.zazor.data.storage.models.toDomain

interface PhotoRepository {

    suspend fun savePhoto(photo: Photo)

    suspend fun deletePhoto(photo: Photo): List<Photo>

    suspend fun getPhotos(): List<Photo>

    suspend fun getPhoto(path: String): Photo?

    suspend fun getLastPhoto(): Photo?

    fun clear()
}

class PhotoRepositoryImpl(private val dao: PhotosDao) : PhotoRepository {

    override suspend fun savePhoto(photo: Photo) =
          dao.savePhoto(photo.toDb())

    override suspend fun getPhotos(): List<Photo> =
          dao.getAll().map { it.toDomain() }

    override suspend fun getPhoto(path: String): Photo? =
        dao.getPhoto(path)?.toDomain()

    override suspend fun getLastPhoto(): Photo? =
        dao.getLast().firstOrNull()?.toDomain()

    override suspend fun deletePhoto(photo: Photo): List<Photo> {
        dao.delete(photo.toDb())
        return getPhotos()
    }

    override fun clear() {
        dao.clear()
    }
}