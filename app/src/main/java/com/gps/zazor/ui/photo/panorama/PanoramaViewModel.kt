package com.gps.zazor.ui.photo.panorama

import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.utils.MetadataStripper
import com.gps.zazor.utils.location.AddressResolver
import com.gps.zazor.utils.location.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import com.gps.zazor.utils.time.PhotoClock

interface PanoramaViewModel : BaseViewModel<PanoramaContract.State, PanoramaContract.Event>

class PanoramaViewModelImpl(
    private val photoRepository: PhotoRepository,
    private val locationProvider: LocationProvider,
    private val addressResolver: AddressResolver
) : BaseViewModelImpl<PanoramaContract.State, PanoramaContract.Event>(), PanoramaViewModel {

    private var locationJob: Job? = null

    private var lastLocation: android.location.Location? = null

    override suspend fun initialState(): PanoramaContract.State? = null

    override fun init() {
        super.init()
        if (locationJob?.isActive != true) {
            locationJob = launch {
                locationProvider.locations().collect { lastLocation = it }
            }
        }
    }

    override fun onEventArrived(event: PanoramaContract.Event?) {
        when (event) {
            // Panoramas used to be stitched to disk and then forgotten - they never reached the
            // gallery because nothing wrote them to the database.
            is PanoramaContract.Event.PanoramaSaved -> launchIo {
                // The stitching SDK copies Make / Model / Software / GPS EXIF from the source
                // frames into the result. Scrub it before the file becomes shareable.
                MetadataStripper.strip(event.path)
                photoRepository.savePhoto(
                    Photo(
                        path = event.path,
                        name = "",
                        date = PhotoClock.now(),
                        address = withContext(Dispatchers.IO) { addressResolver.resolve(lastLocation) }.orEmpty(),
                        lat = lastLocation?.latitude,
                        lng = lastLocation?.longitude
                    )
                )
                uiState.value = PanoramaContract.State.Saved
            }
            else -> Unit
        }
    }

    override fun onCleared() {
        locationJob?.cancel()
        super.onCleared()
    }
}
