package com.gps.zazor.ui.settings.support

import android.net.Uri
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class SupportContract {

    /**
     * What the letter is about.
     *
     * Four, and no more: it decides who reads the letter first and how fast, and a list of
     * twenty categories is a list nobody reads to the end.
     */
    enum class Topic {
        BUG, IDEA, PAYMENT, OTHER
    }

    sealed class Event : UiEvent {

        data class TopicSelected(val topic: Topic) : Event()

        data class DeviceInfoToggled(val on: Boolean) : Event()

        /** The person picked an image to attach, or cleared the one they had picked. */
        data class ScreenshotPicked(val uri: Uri?) : Event()
    }

    sealed class State : UiState {

        data class Form(
            val topic: Topic = Topic.BUG,
            val includeDeviceInfo: Boolean = true,
            val screenshot: Uri? = null
        ) : State()
    }
}
