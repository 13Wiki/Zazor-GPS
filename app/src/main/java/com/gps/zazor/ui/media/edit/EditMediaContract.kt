package com.gps.zazor.ui.media.edit

import android.graphics.Bitmap
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import com.gps.zazor.views.Mode

class EditMediaContract {

    sealed class Event : UiEvent {

        data class Initial(val path: String) : Event()

        class SaveEdits(val bitmap: Bitmap) : EditMediaContract.Event()
    }

    sealed class State : UiState {

        object SaveNotes : EditMediaContract.State()

        class AddNotes(val notes: String,
                       val lat: String?,
                       val long: String?,
                       val date: String?,
                       val time: String?,
                       val accuracy: String?) : EditMediaContract.State()

        class AddOverlay(val text: String?,
                         val color: Int?,
                         val fontId: Int?) : EditMediaContract.State()

        data class AllowDraw(val color: Int?, val mode: Mode) : EditMediaContract.State()

        object Done : State()

        object DisallowDraw : EditMediaContract.State()

        object ClearDraw : EditMediaContract.State()
    }
}