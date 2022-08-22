package com.gps.zazor.ui.photo.collage.base

import android.graphics.Bitmap
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class CollageContract {

    sealed class Event : UiEvent {

        data class Initial(val gridSize: Int) : Event()

        object Resume : Event()

        object PreviewShown : Event()

        class SaveEdits(val bitmap: Bitmap) : Event()

        object CapturePressed : Event()
    }

    sealed class State : UiState {

        data class ShowPreview(val bitmap: Bitmap, val index: Int) : State()

        object AllowCollageCapture : State()

        object DisallowCollageCapture : State()

        object CaptureCollage : State()
    }
}