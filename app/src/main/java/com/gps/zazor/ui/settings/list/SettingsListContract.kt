package com.gps.zazor.ui.settings.list

import com.gps.zazor.data.models.MainSetting
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class SettingsListContract {

    sealed class Event : UiEvent

    sealed class State : UiState {
        data class Initial(val settings: List<MainSetting>) : State()
    }
}