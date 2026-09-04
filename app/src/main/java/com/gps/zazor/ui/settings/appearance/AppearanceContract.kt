package com.gps.zazor.ui.settings.appearance

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import com.gps.zazor.utils.launcher.LauncherAppearance

class AppearanceContract {

    sealed class Event : UiEvent {

        data class Choose(val appearance: LauncherAppearance.Appearance) : Event()
    }

    sealed class State : UiState {

        data class Content(
            val current: LauncherAppearance.Appearance,
            val hasWipeCode: Boolean
        ) : State()
    }
}
