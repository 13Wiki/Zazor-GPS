package com.gps.zazor.ui.auth.pin

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

const val PIN_LENGTH = 5

class AuthPinContract {

    sealed class Event : UiEvent {

        data class PinEntered(val pin: String) : Event()
    }

    sealed class State : UiState {

        object AuthSuccess : State()
        object DataCleared : State()
        object AuthFailure : State()
    }
}