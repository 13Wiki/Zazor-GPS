package com.gps.zazor.data.repositories

import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.models.needsAddress
import com.gps.zazor.data.models.toDb
import com.gps.zazor.data.storage.dao.PhotosDao
import com.gps.zazor.data.storage.models.toDomain
import com.gps.zazor.utils.PhotoStorage
import com.gps.zazor.utils.location.AddressResolver

interface PhotoRepository {

    suspend fun savePhoto(photo: Photo)

    suspend fun deletePhoto(photo: Photo): List<Photo>

    suspend fun getPhotos(): List<Photo>

    suspend fun getPhoto(path: String): Photo?

    suspend fun getLastPhoto(): Photo?

    suspend fun attachVoiceNote(path: String, voiceNotePath: String?)

    /**
     * Resolves addresses for photos taken while offline.
     *
     * @return how many rows were filled in.
     */
    suspend fun backfillAddresses(): Int

    suspend fun clear()
}

class PhotoRepositoryImpl(
    private val dao: PhotosDao,
    private val storage: PhotoStorage,
    private val addressResolver: AddressResolver
) : PhotoRepository {

    override suspend fun savePhoto(photo: Photo) = dao.savePhoto(photo.toDb())

    override suspend fun getPhotos(): List<Photo> = dao.getAll().map { it.toDomain() }

    override suspend fun getPhoto(path: String): Photo? = dao.getPhoto(path)?.toDomain()

    override suspend fun getLastPhoto(): Photo? = dao.getLast()?.toDomain()

    override suspend fun attachVoiceNote(path: String, voiceNotePath: String?) {
        // Drop the previous recording so re-recording does not leave orphans on disk.
        dao.getPhoto(path)?.voiceNotePath
            ?.takeIf { it != voiceNotePath }
            ?.let { storage.delete(it) }
        dao.updateVoiceNote(path, voiceNotePath)
    }

    /**
     * A photo taken without connectivity is stored with an empty address; the geocoder is retried
     * later instead of that photo being stuck without one forever.
     */
    override suspend fun backfillAddresses(): Int {
        var filled = 0
        dao.getWithoutAddress().map { it.toDomain() }.forEach { photo ->
            if (!photo.needsAddress) return@forEach
            val address = addressResolver.resolve(photo.lat, photo.lng)
            if (!address.isNullOrBlank()) {
                dao.updateAddress(photo.path, address)
                filled++
            }
        }
        return filled
    }

    override suspend fun deletePhoto(photo: Photo): List<Photo> {
        dao.delete(photo.toDb())
        // The row was the only thing removed before, leaving the JPEG behind forever.
        storage.delete(photo.path)
        photo.voiceNotePath?.let { storage.delete(it) }
        return getPhotos()
    }

    override suspend fun clear() {
        getPhotos().forEach { photo ->
            storage.delete(photo.path)
            photo.voiceNotePath?.let { storage.delete(it) }
        }
        dao.clear()
    }
}
