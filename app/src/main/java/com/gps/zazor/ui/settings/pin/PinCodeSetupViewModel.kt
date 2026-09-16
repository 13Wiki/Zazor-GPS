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

    open fun hasCode(): Boolean = prefs.hasPin()

    open fun isCode(code: String): Boolean = prefs.isPin(code)

    override suspend fun initialState(): PinCodeSetupContract.State? = null

    override fun onEventArrived(event: PinCodeSetupContract.Event?) {
        when (event) {
            is PinCodeSetupContract.Event.CodeEntered -> checkPin(event.pin)
            else -> Unit
        }
    }

    /**
     * Sets the code when there is none, and removes the existing one when it is entered again.
     *
     * Asks whether the code matches rather than reading it back: codes are stored as salted
     * hashes, and nothing in the app ever needs to see one.
     */
    private fun checkPin(pin: String) {
        uiState.value = when {
            pin.length != PIN_LENGTH -> PinCodeSetupContract.State.CodeIncorrect
            !hasCode() -> {
                setCode(pin)
                PinCodeSetupContract.State.CodeSet
            }
            isCode(pin) -> {
                setCode(null)
                PinCodeSetupContract.State.CodeSet
            }
            else -> PinCodeSetupContract.State.CodeIncorrect
        }
    }
}
