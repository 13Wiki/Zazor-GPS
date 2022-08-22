package com.gps.zazor.ui.settings

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class SettingsContract {

    sealed class Event : UiEvent

    sealed class State : UiState {
        object Initial : State()
    }
}