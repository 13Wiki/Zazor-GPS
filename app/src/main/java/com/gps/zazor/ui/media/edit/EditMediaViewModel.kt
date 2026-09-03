package com.gps.zazor.ui.media.edit

import android.graphics.Bitmap
import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.utils.time.PhotoClock
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.utils.PhotoStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Locale

interface EditMediaViewModel : BaseViewModel<EditMediaContract.State, EditMediaContract.Event>

class EditMediaViewModelImpl(
    private val editPhotoFlow: MutableSharedFlow<EditPhotoContract.Flow>,
    private val photosRepository: PhotoRepository,
    private val photoStorage: PhotoStorage,
    private val prefs: AppPreferences
) : BaseViewModelImpl<EditMediaContract.State, EditMediaContract.Event>(), EditMediaViewModel {

    private var photo: Photo? = null

    private var noteJob: Job? = null

    override suspend fun initialState(): EditMediaContract.State? = null

    override fun onEventArrived(event: EditMediaContract.Event?) {
        when (event) {
            is EditMediaContract.Event.Initial -> {
                subscribeToAddNoteFlow()
                launchIo {
                    photo = photosRepository.getPhoto(event.path)
                }
            }
            is EditMediaContract.Event.SaveEdits -> saveEdits(event.bitmap)
            else -> Unit
        }
    }

    override fun onCleared() {
        noteJob?.cancel()
        super.onCleared()
    }

    private fun subscribeToAddNoteFlow() {
        if (noteJob?.isActive == true) return
        noteJob = launch {
            editPhotoFlow.collect { flowState ->
                when (flowState) {
                    is EditPhotoContract.Flow.Cancel -> uiState.value = EditMediaContract.State.Done
                    is EditPhotoContract.Flow.AddNote -> handleNoteAdding(flowState)
                    is EditPhotoContract.Flow.AddOverlay -> flowState.run {
                        uiState.value = EditMediaContract.State.AddOverlay(text, color, fontId)
                    }
                    is EditPhotoContract.Flow.Done -> uiState.value = EditMediaContract.State.SaveNotes
                    is EditPhotoContract.Flow.AllowPaint ->
                        uiState.value = EditMediaContract.State.AllowDraw(flowState.color, flowState.mode)
                    is EditPhotoContract.Flow.DisallowPaint ->
                        uiState.value = EditMediaContract.State.DisallowDraw
                    is EditPhotoContract.Flow.ClearPaint ->
                        uiState.value = EditMediaContract.State.ClearDraw
                    is EditPhotoContract.Flow.Idle -> Unit
                }
            }
        }
    }

    /**
     * Renders the note even when the stored photo carries no coordinates - the old version bailed
     * out silently, so typing a note on such a picture did nothing.
     */
    private fun handleNoteAdding(flowState: EditPhotoContract.Flow.AddNote) {
        val current = photo ?: return
        val hasCoordinates = current.lat != null && current.lng != null
        uiState.value = EditMediaContract.State.AddNotes(
            flowState.note,
            current.lat?.formatCoordinate().takeIf { prefs.isDisplayCoordinates() && hasCoordinates },
            current.lng?.formatCoordinate().takeIf { prefs.isDisplayCoordinates() && hasCoordinates },
            PhotoClock.formatDate(current.date).takeIf { prefs.isDisplayDate() },
            PhotoClock.formatTime(current.date).takeIf { prefs.isDisplayTime() },
            null
        )
    }

    private fun saveEdits(bitmap: Bitmap) {
        launchIo {
            photo?.path?.let { photoStorage.save(bitmap, it) }
            uiState.value = EditMediaContract.State.Done
        }
    }

    private fun Double.formatCoordinate() = String.format(Locale.US, "%.6f", this)
}
