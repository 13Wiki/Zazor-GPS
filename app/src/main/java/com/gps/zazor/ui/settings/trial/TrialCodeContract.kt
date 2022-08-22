package com.gps.zazor.ui.settings.trial

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

const val TRIAL_CODE = "00000"

class TrialCodeContract {

    sealed class Event : UiEvent {

        data class CodeEntered(val code: String) : Event()
    }

    sealed class State : UiState {

        object CodeIncorrect : State()
        object CodeSet : State()
    }
}