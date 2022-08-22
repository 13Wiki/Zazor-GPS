package com.gps.zazor.ui.photo

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class PhotoContract {

    sealed class Event : UiEvent {

        class PermissionResult(val granted: Boolean) : Event()

        object EditPhotoClosed : Event()
    }

    sealed class State : UiState {
        data class Initial(val photoUri: String?) : State()

        object PermissionGranted : State()

        object PermissionDenied : State()
    }
}