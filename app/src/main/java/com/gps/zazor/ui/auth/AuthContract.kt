package com.gps.zazor.ui.auth

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class AuthContract {

    sealed class Event : UiEvent {

        /** Re-checks what the launch screen should show, after the first-run screen is done. */
        object Recheck : Event()
    }

    sealed class State : UiState {

        /** Shown once, before anything else: what the app does and does not collect. */
        object NeedsPrivacy : State()

        data class Initial(val needAuth: Boolean) : State()
    }
}
