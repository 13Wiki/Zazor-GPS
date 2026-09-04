package com.gps.zazor.ui.privacy

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class PrivacyContract {

    sealed class Event : UiEvent {

        data class ToggleAnalytics(val enabled: Boolean) : Event()

        object Accept : Event()
    }

    sealed class State : UiState {

        data class Content(val analyticsEnabled: Boolean) : State()

        object Accepted : State()
    }
}
