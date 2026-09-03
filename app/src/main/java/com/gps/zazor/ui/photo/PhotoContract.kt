package com.gps.zazor.ui.photo

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class PhotoContract {

    sealed class Event : UiEvent {

        class PermissionResult(val granted: Boolean) : Event()

        object EditPhotoClosed : Event()
    }

    sealed class State : UiState {

        /**
         * One conflatable state carrying everything the screen renders.
         *
         * Splitting the thumbnail and the permission result into separate states meant a
         * [kotlinx.coroutines.flow.StateFlow] update could conflate one away and leave the screen
         * either without its camera pager or without its gallery thumbnail.
         *
         * @param isPermissionGranted `null` while the request is still pending.
         */
        data class Content(val photoUri: String?, val isPermissionGranted: Boolean?) : State()
    }
}
