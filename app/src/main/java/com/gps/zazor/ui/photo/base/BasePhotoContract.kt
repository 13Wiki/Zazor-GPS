package com.gps.zazor.ui.photo.base

import android.graphics.Bitmap
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import com.gps.zazor.utils.camera.Camera
import com.gps.zazor.views.Mode
import io.fotoapparat.configuration.UpdateConfiguration
import io.fotoapparat.result.BitmapPhoto

const val TRIAL_COUNT = 5

class BasePhotoContract {

    sealed class Event : UiEvent {

        class CollageInitial(val index: Int) : Event()

        object Resume : Event()

        class PhotoCaptured(val photo: BitmapPhoto) : Event()

        class SaveEdits(val bitmap: Bitmap) : Event()

        object FlipCamera : Event()

        object ToggleFlash : Event()

        object BackPressed : Event()

        object Pause : Event()

        object Stop : Event()
    }

    sealed class State : UiState {

        data class Initial(val isTrial: Boolean) : State()

        class FlipCamera(val configuration: Camera) : State()

        class ToggleFlash(val config: UpdateConfiguration) : State()

        class ShowPreview(val photo: BitmapPhoto, val notes: AddNotes) : State()

        object SaveNotes : State()

        class AddNotes(val notes: String?,
                       val lat: String?,
                       val long: String?,
                       val date: String?,
                       val time: String?,
                       val accuracy: String?) : State()

        class AddOverlay(val text: String?,
                         val color: Int?,
                         val fontId: Int?) : State()

        data class AllowDraw(val color: Int?, val mode: Mode) : State()

        object DisallowDraw : State()

        object ClearDraw : State()

        data class HidePreview(val isTrial: Boolean) : State()

        object Exit : State()
    }
}