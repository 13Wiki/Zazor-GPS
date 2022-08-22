package com.gps.zazor.ui.photo.collage.base

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.ui.photo.base.BasePhotoViewModelImpl
import com.gps.zazor.ui.photo.collage.photo.CollagePhoto
import com.gps.zazor.utils.extensions.toBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException

interface CollageViewModel : BaseViewModel<CollageContract.State, CollageContract.Event>

class CollageViewModelImpl(private val context: Context,
                           private val collagePhotoFlow: MutableStateFlow<CollagePhoto?>,
                           private val photoRepository: PhotoRepository
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
                }
            }
        }
    }

    override suspend fun initialState(): CollageContract.State? = null

    override fun onEventArrived(event: CollageContract.Event?) {
        when (event) {
            is CollageContract.Event.Initial -> {
                gridSize = event.gridSize
            }
            is CollageContract.Event.Resume -> {
                handleCaptureState()
            }
            is CollageContract.Event.PreviewShown -> {
                if (++photoCounter >= gridSize) {
                    uiState.value = CollageContract.State.AllowCollageCapture
                }
            }
            is CollageContract.Event.SaveEdits -> {
                saveEdits(event.bitmap)
            }
            is CollageContract.Event.CapturePressed -> {
                photoCounter = 0
                uiState.value = CollageContract.State.CaptureCollage
            }
        }
    }

    private fun handleCaptureState() {
        uiState.value =
            if (photoCounter >= gridSize)
                CollageContract.State.AllowCollageCapture
            else
                CollageContract.State.DisallowCollageCapture
    }

    private fun saveEdits(bitmap: Bitmap) {
        launch {
            val localPath = loadToLocalFile(bitmap.toBytes(), null)
            savePhoto(localPath)
        }
    }

    private suspend fun savePhoto(path: String) {
        photoRepository.savePhoto(
            Photo(
                path,
                "",
                DateTime.now(),
                address,
                lat,
                lng
            )
        )
    }

    /**
     * @return absolute path to stored local file.
     */
    private fun loadToLocalFile(packet: ByteArray, name: String?): String =
        File(
            context.getExternalFilesDir(BasePhotoViewModelImpl.DIR_PHOTOS),
            (name ?: DateTime.now().millis.toString()) + ".jpg"
        ).also { file ->
            saveImage(packet, file.outputStream().buffered())
        }.absolutePath

    @Throws(IOException::class)
    private fun saveImage(bytes: ByteArray, outputStream: BufferedOutputStream) {
        outputStream.use {
            it.write(bytes)
            it.flush()
        }
    }
}