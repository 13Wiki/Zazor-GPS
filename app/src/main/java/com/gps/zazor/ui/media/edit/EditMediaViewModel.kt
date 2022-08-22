package com.gps.zazor.ui.media.edit

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.models.location
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.utils.extensions.toBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException

interface EditMediaViewModel : BaseViewModel<EditMediaContract.State, EditMediaContract.Event>

class EditMediaViewModelImpl(private val context: Context,
                             private val editPhotoFlow: MutableSharedFlow<EditPhotoContract.Flow>,
                             private val photosRepository: PhotoRepository,
                             private val prefs: AppPreferences)
    : BaseViewModelImpl<EditMediaContract.State, EditMediaContract.Event>(), EditMediaViewModel {

    private var photo: Photo? = null

    init {
        subscribeToAddNoteFlow()
    }

    override suspend fun initialState(): EditMediaContract.State? = null

    override fun onEventArrived(event: EditMediaContract.Event?) {
        when (event) {
            is EditMediaContract.Event.Initial -> {
                viewModelScope.launch(Dispatchers.IO) {
                    photo = photosRepository.getPhoto(event.path)
                }
            }
            is EditMediaContract.Event.SaveEdits -> saveEdits(event.bitmap)
        }
    }

    private fun subscribeToAddNoteFlow() {
        viewModelScope.launch {
            editPhotoFlow.collect { flowState ->
                when (flowState) {
                    is EditPhotoContract.Flow.Cancel -> {
                        uiState.value = EditMediaContract.State.Done
                    }
                    is EditPhotoContract.Flow.AddNote -> {
                        handleNoteAdding(flowState)
                        //handleBackPressed()
                        //addNoteFlow.value = AddNoteContract.Flow.Idle
                    }
                    is EditPhotoContract.Flow.AddOverlay -> {
                        handleOverlayAdding(flowState)
                    }
                    is EditPhotoContract.Flow.Done -> {
                        uiState.value = EditMediaContract.State.SaveNotes
                        //savePhoto()
                    }
                    is EditPhotoContract.Flow.AllowPaint -> {
                        uiState.value = EditMediaContract.State.AllowDraw(flowState.color, flowState.mode)
                    }
                    is EditPhotoContract.Flow.DisallowPaint -> {
                        uiState.value = EditMediaContract.State.DisallowDraw
                    }
                    is EditPhotoContract.Flow.ClearPaint -> {
                        uiState.value = EditMediaContract.State.ClearDraw
                    }
                }
            }
        }
    }

    private fun handleOverlayAdding(flowState: EditPhotoContract.Flow.AddOverlay) {
        flowState.run {
            uiState.value = EditMediaContract.State.AddOverlay(text, color, fontId)
        }
    }

    private fun handleNoteAdding(flowState: EditPhotoContract.Flow.AddNote) {
        photo?.location?.let { location ->
            photo?.date?.let { date ->
                uiState.value = EditMediaContract.State.AddNotes(
                    flowState.note,
                    location.latitude.toString().takeIf { prefs.isDisplayCoordinates() },
                    location.longitude.toString().takeIf { prefs.isDisplayCoordinates() },
                    date.toString("dd.MM.YYYY").takeIf { prefs.isDisplayDate() },
                    date.toString("hh:mm").takeIf { prefs.isDisplayTime() },
                    location.accuracy.toInt().toString().takeIf { prefs.isDisplayAccuracy() }
                )
            }
        }
    }

    private fun saveEdits(bitmap: Bitmap) {
        launch {
            loadToLocalFile(bitmap.toBytes(), photo?.path)
            uiState.value = EditMediaContract.State.Done
        }
    }

    /**
     * @return absolute path to stored local file.
     */
    private fun loadToLocalFile(packet: ByteArray, path: String?): String? =
        path?.let(::File)?.also { file ->
            saveImage(packet, file.outputStream().buffered())
        }?.absolutePath

    @Throws(IOException::class)
    private fun saveImage(bytes: ByteArray, outputStream: BufferedOutputStream) {
        outputStream.use {
            it.write(bytes)
            it.flush()
        }
    }
}