package com.gps.zazor.ui.photo.base

import android.graphics.Bitmap
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import com.gps.zazor.utils.camera.Camera
import com.gps.zazor.views.Mode

class BasePhotoContract {

    sealed class Event : UiEvent {

        class CollageInitial(val index: Int) : Event()

        object Resume : Event()

        class PhotoCaptured(val photo: Bitmap) : Event()

        /** @param isWide the frame came from the wide lens, which the gallery filters by. */
        class SaveEdits(val bitmap: Bitmap, val isWide: Boolean = false) : Event()

        object FlipCamera : Event()

        object ToggleFlash : Event()

        object BackPressed : Event()

        object Pause : Event()

        object Stop : Event()

        /** Starts a new approach series, or closes the open one. */
        object ToggleSeries : Event()

        /** Re-opens a series shot earlier, so the next frames join it. */
        class ResumeSeries(val seriesId: String) : Event()
    }

    sealed class State : UiState {

        object Initial : State()

        class FlipCamera(val camera: Camera) : State()

        class ToggleFlash(val isOn: Boolean) : State()

        class ShowPreview(val bitmap: Bitmap, val notes: AddNotes) : State()

        object SaveNotes : State()

        class AddNotes(val notes: String?,
                       val lat: String?,
                       val long: String?,
                       val date: String?,
                       val time: String?,
                       val accuracy: String?,
                       /** Degrees clockwise from north, or null when the phone has no compass. */
                       val bearing: Float? = null) : State()

        class AddOverlay(val text: String?,
                         val color: Int?,
                         val fontId: Int?) : State()

        data class AllowDraw(val color: Int?, val mode: Mode) : State()

        object DisallowDraw : State()

        object ClearDraw : State()

        object UndoDraw : State()

        object HidePreview : State()

        object Exit : State()
    }
}
