package com.gps.zazor.ui.photo.collage.base

import android.graphics.Bitmap
import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.ui.photo.collage.photo.CollagePhoto
import com.gps.zazor.utils.PhotoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.gps.zazor.utils.time.PhotoClock

interface CollageViewModel : BaseViewModel<CollageContract.State, CollageContract.Event>

class CollageViewModelImpl(
    private val collagePhotoFlow: MutableStateFlow<CollagePhoto?>,
    private val photoRepository: PhotoRepository,
    private val photoStorage: PhotoStorage
) : BaseViewModelImpl<CollageContract.State, CollageContract.Event>(), CollageViewModel {

    private var photoCounter = 0

    private var gridSize = 0

    private var address = ""
    private var lat = 0.0
    private var lng = 0.0

    init {
        viewModelScope.launch {
            collagePhotoFlow.collect { photo ->
                photo?.let {
                    uiState.value = CollageContract.State.ShowPreview(it.bitmap, it.index)
                    address = it.address
                    lat = it.lat
                    lng = it.lng
                    // Consume the cell so re-collecting after a rotation does not replay it.
                    collagePhotoFlow.value = null
                }
            }
        }
    }

    override suspend fun initialState(): CollageContract.State? = null

    override fun onEventArrived(event: CollageContract.Event?) {
        when (event) {
            is CollageContract.Event.Initial -> {
                // Switching between grid shapes resets progress, otherwise a stale counter can
                // enable the capture button for a layout that has empty cells.
                if (gridSize != event.gridSize) {
                    gridSize = event.gridSize
                    photoCounter = 0
                }
            }
            is CollageContract.Event.Resume -> handleCaptureState()
            is CollageContract.Event.PreviewShown -> {
                if (++photoCounter >= gridSize) {
                    uiState.value = CollageContract.State.AllowCollageCapture
                }
            }
            is CollageContract.Event.SaveEdits -> saveEdits(event.bitmap)
            is CollageContract.Event.CapturePressed -> {
                photoCounter = 0
                uiState.value = CollageContract.State.CaptureCollage
            }
            else -> Unit
        }
    }

    private fun handleCaptureState() {
        uiState.value =
            if (gridSize > 0 && photoCounter >= gridSize) CollageContract.State.AllowCollageCapture
            else CollageContract.State.DisallowCollageCapture
    }

    private fun saveEdits(bitmap: Bitmap) {
        launchIo {
            photoStorage.save(bitmap)?.let { path ->
                photoRepository.savePhoto(Photo(path, "", PhotoClock.now(), address, lat, lng))
            }
        }
    }
}
