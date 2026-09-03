package com.gps.zazor.ui.photo.panorama

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class PanoramaContract {

    sealed class Event : UiEvent {

        /** Emitted once the library has written the stitched panorama to [path]. */
        data class PanoramaSaved(val path: String) : Event()
    }

    sealed class State : UiState {

        object Saved : State()
    }
}
