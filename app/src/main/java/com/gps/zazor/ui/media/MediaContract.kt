package com.gps.zazor.ui.media

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class MediaContract {

    sealed class Event : UiEvent

    sealed class State : UiState {
        object Initial : State()
    }
}