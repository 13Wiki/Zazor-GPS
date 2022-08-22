package com.gps.zazor.ui.settings.pin

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class PinCodeSetupContract {

    sealed class Event : UiEvent {

        data class CodeEntered(val pin: String) : Event()
    }

    sealed class State : UiState {

        object CodeIncorrect : State()
        object CodeSet : State()
    }
}