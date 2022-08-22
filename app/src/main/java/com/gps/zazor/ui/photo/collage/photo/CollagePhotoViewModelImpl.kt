package com.gps.zazor.ui.photo.collage.photo

import android.content.Context
import android.graphics.Bitmap
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.photo.base.BasePhotoContract
import com.gps.zazor.ui.photo.base.BasePhotoViewModelImpl
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

data class CollagePhoto(val bitmap: Bitmap,
                        val index: Int,
                        val lat: Double,
                        val lng: Double,
                        val address: String)

class CollagePhotoViewModelImpl(context: Context,
                                private val collagePhotoFlow: MutableStateFlow<CollagePhoto?>,
                                editPhotoFlow: MutableSharedFlow<EditPhotoContract.Flow>,
                                prefs: AppPreferences,
                                photoRepository: PhotoRepository)
    : BasePhotoViewModelImpl(context, editPhotoFlow, prefs, photoRepository) {

    private var index = -1

    override fun onEventArrived(event: BasePhotoContract.Event?) {
        when (event) {
            is BasePhotoContract.Event.CollageInitial -> {
                index = event.index
            }
            else -> super.onEventArrived(event)
        }
    }

    override fun handleBackPressed() {
        uiState.value = BasePhotoContract.State.Exit
    }

    override fun onSaveEdits(edits: BasePhotoContract.Event.SaveEdits) {
        collagePhotoFlow.value = CollagePhoto(edits.bitmap, index,
            lastLocation?.latitude ?: 0.0,
            lastLocation?.longitude ?: 0.0,
            getAddress().orEmpty())
        handleBackPressed()
    }
}