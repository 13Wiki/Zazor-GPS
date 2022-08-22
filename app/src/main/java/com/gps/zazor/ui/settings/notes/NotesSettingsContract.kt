package com.gps.zazor.ui.settings.notes

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class NotesSettingsContract {

    sealed class Event : UiEvent {

        data class CoordinatesSwitched(val isChecked: Boolean): Event()
        data class DateSwitched(val isChecked: Boolean): Event()
        data class TimeSwitched(val isChecked: Boolean): Event()
        data class AccuracySwitched(val isChecked: Boolean): Event()
    }

    sealed class State : UiState {

        data class Initial(val displayCoordinates: Boolean,
                           val displayDate: Boolean,
                           val displayTime: Boolean,
                           val displayAccuracy: Boolean) : State()
    }
}