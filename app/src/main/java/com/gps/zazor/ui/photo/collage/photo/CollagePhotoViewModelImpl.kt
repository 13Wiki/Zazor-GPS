package com.gps.zazor.ui.photo.collage.photo

import android.graphics.Bitmap
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.photo.base.BasePhotoContract
import com.gps.zazor.ui.photo.base.BasePhotoViewModelImpl
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.utils.PhotoStorage
import com.gps.zazor.utils.location.AddressResolver
import com.gps.zazor.utils.location.LocationProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

data class CollagePhoto(val bitmap: Bitmap,
                        val index: Int,
                        val lat: Double,
                        val lng: Double,
                        val address: String)

class CollagePhotoViewModelImpl(
    private val collagePhotoFlow: MutableStateFlow<CollagePhoto?>,
    editPhotoFlow: MutableSharedFlow<EditPhotoContract.Flow>,
    prefs: AppPreferences,
    photoRepository: PhotoRepository,
    photoStorage: PhotoStorage,
    locationProvider: LocationProvider,
    addressResolver: AddressResolver
) : BasePhotoViewModelImpl(
    editPhotoFlow, prefs, photoRepository, photoStorage, locationProvider, addressResolver
) {

    private var index = -1

    override fun onEventArrived(event: BasePhotoContract.Event?) {
        when (event) {
            is BasePhotoContract.Event.CollageInitial -> index = event.index
            else -> super.onEventArrived(event)
        }
    }

    override fun handleBackPressed() {
        uiState.value = BasePhotoContract.State.Exit
    }

    /**
     * A collage cell is handed back to the grid instead of being written to disk on its own.
     * The reverse geocode runs off the main thread - it used to block it with a network call.
     */
    override fun onSaveEdits(edits: BasePhotoContract.Event.SaveEdits) {
        launchIo {
            val address = resolveAddress().orEmpty()
            val location = lastLocation
            collagePhotoFlow.value = CollagePhoto(
                edits.bitmap,
                index,
                location?.latitude ?: 0.0,
                location?.longitude ?: 0.0,
                address
            )
            handleBackPressed()
        }
    }
}
