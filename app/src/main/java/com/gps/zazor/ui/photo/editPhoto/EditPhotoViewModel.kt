package com.gps.zazor.ui.photo.editPhoto

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.views.Mode
import kotlinx.coroutines.flow.MutableSharedFlow

interface EditPhotoViewModel : BaseViewModel<EditPhotoContract.State, EditPhotoContract.Event>

class EditPhotoViewModelImpl(private val editPhotoFlow: MutableSharedFlow<EditPhotoContract.Flow>,
                             private val prefs: AppPreferences) : BaseViewModelImpl<EditPhotoContract.State, EditPhotoContract.Event>(), EditPhotoViewModel {

    override suspend fun initialState(): EditPhotoContract.State = EditPhotoContract.State.NotesScreen

    private lateinit var currentPaintMode: Mode

    override fun onEventArrived(event: EditPhotoContract.Event?) {
        when (event) {
            is EditPhotoContract.Event.NotesTabPressed -> {
                uiState.value = EditPhotoContract.State.NotesScreen
                disallowPaint()
            }
            is EditPhotoContract.Event.PaintTabPressed -> {
                uiState.value = EditPhotoContract.State.PaintScreen(prefs.getDrawColor())
                launch {
                    currentPaintMode = event.paintMode
                    editPhotoFlow.emit(EditPhotoContract.Flow.AllowPaint(null, event.paintMode))
                }
            }
            is EditPhotoContract.Event.TextTabPressed -> {
                uiState.value = EditPhotoContract.State.TextScreen(prefs.getTextColor(), prefs.getFont())
                disallowPaint()
            }
            is EditPhotoContract.Event.DonePressed -> {
                launch {
                    editPhotoFlow.emit(EditPhotoContract.Flow.Done)
                }
            }
            is EditPhotoContract.Event.CancelPressed -> {
                launch {
                    editPhotoFlow.emit(EditPhotoContract.Flow.Cancel)
                }
            }
            is EditPhotoContract.Event.NotesEntered -> {
                launch {
                    editPhotoFlow.emit(EditPhotoContract.Flow.AddNote(event.notes))
                }
            }
            is EditPhotoContract.Event.PaintColorPicked -> {
                launch {
                    prefs.putDrawColor(event.color)
                    editPhotoFlow.emit(EditPhotoContract.Flow.AllowPaint(event.color, currentPaintMode))
                }
            }
            is EditPhotoContract.Event.OverlayEntered -> {
                launch {
                    event.color?.let(prefs::putTextColor)
                    event.fontId?.let(prefs::putFont)
                    editPhotoFlow.emit(EditPhotoContract.Flow.AddOverlay(
                        event.text, event.color, event.fontId))
                }
            }
            is EditPhotoContract.Event.ClearPaint -> {
                launch {
                    editPhotoFlow.emit(EditPhotoContract.Flow.ClearPaint)
                }
            }
        }
    }

    private fun disallowPaint() {
        launch {
            editPhotoFlow.emit(EditPhotoContract.Flow.DisallowPaint)
        }
    }
}