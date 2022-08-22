package com.gps.zazor.ui.settings.pin

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.auth.pin.PIN_LENGTH
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface PinCodeSetupViewModel : BaseViewModel<PinCodeSetupContract.State, PinCodeSetupContract.Event>

open class PinCodeSetupViewModelImpl(protected val prefs: AppPreferences) : BaseViewModelImpl<PinCodeSetupContract.State, PinCodeSetupContract.Event>(), PinCodeSetupViewModel {

    open fun setCode(code: String?) {
        prefs.putPin(code)
    }

    open fun getCode(): String? = prefs.getPin()

    override suspend fun initialState(): PinCodeSetupContract.State? = null

    override fun onEventArrived(event: PinCodeSetupContract.Event?) {
        when (event) {
            is PinCodeSetupContract.Event.CodeEntered -> checkPin(event.pin)
        }
    }

    private fun checkPin(pin: String) {
        if (pin.length != PIN_LENGTH) {
            uiState.value = PinCodeSetupContract.State.CodeIncorrect
        } else {
            getCode()?.let {
                if (pin != it) {
                    uiState.value = PinCodeSetupContract.State.CodeIncorrect
                } else {
                    // clear old pin
                    setCode(null)
                    uiState.value = PinCodeSetupContract.State.CodeSet
                }
            } ?: run {
                setCode(pin)
                uiState.value = PinCodeSetupContract.State.CodeSet
            }
        }
    }
}