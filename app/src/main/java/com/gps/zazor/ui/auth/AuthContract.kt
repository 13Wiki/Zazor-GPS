package com.gps.zazor.ui.auth

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class AuthContract {

    sealed class Event : UiEvent

    sealed class State : UiState {
        data class Initial(val needAuth: Boolean) : State()
    }
}