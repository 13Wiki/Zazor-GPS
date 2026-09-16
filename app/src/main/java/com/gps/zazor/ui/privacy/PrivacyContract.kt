package com.gps.zazor.ui.privacy

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class PrivacyContract {

    sealed class Event : UiEvent {

        data class ToggleAnalytics(val enabled: Boolean) : Event()

        object Accept : Event()
    }

    sealed class State : UiState {

        /**
         * @param analyticsAvailable false in a build with no Firebase project: there is nothing to
         *        agree to, so the switch is not shown at all rather than shown and dead.
         */
        data class Content(
            val analyticsEnabled: Boolean,
            val analyticsAvailable: Boolean
        ) : State()

        object Accepted : State()
    }
}
