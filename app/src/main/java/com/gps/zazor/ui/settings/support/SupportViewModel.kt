package com.gps.zazor.ui.settings.support

import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface SupportViewModel : BaseViewModel<SupportContract.State, SupportContract.Event>

/**
 * Holds what the letter will say while it is being written.
 *
 * The text itself stays in the input field - it survives rotation there on its own, and a draft
 * of a complaint is not something to keep anywhere else.
 */
class SupportViewModelImpl :
    BaseViewModelImpl<SupportContract.State, SupportContract.Event>(), SupportViewModel {

    private var form = SupportContract.State.Form()

    override suspend fun initialState(): SupportContract.State = form

    override fun onEventArrived(event: SupportContract.Event?) {
        when (event) {
            is SupportContract.Event.TopicSelected -> update { copy(topic = event.topic) }
            is SupportContract.Event.DeviceInfoToggled -> update { copy(includeDeviceInfo = event.on) }
            is SupportContract.Event.ScreenshotPicked -> update { copy(screenshot = event.uri) }
            else -> Unit
        }
    }

    private fun update(change: SupportContract.State.Form.() -> SupportContract.State.Form) {
        form = form.change()
        uiState.value = form
    }
}
