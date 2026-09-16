package com.gps.zazor.ui.auth.pin

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface AuthPinViewModel : BaseViewModel<AuthPinContract.State, AuthPinContract.Event>

class AuthPinViewModelImpl(
    private val prefs: AppPreferences,
    private val photosRepository: PhotoRepository
) : BaseViewModelImpl<AuthPinContract.State, AuthPinContract.Event>(), AuthPinViewModel {

    override suspend fun initialState(): AuthPinContract.State? = null

    override fun onEventArrived(event: AuthPinContract.Event?) {
        when (event) {
            is AuthPinContract.Event.PinEntered -> launchIo { checkPin(event.pin) }
            else -> Unit
        }
    }

    private suspend fun checkPin(pin: String) {
        if (pin.length != PIN_LENGTH) return
        uiState.value = when (pin) {
            prefs.getPin() -> AuthPinContract.State.AuthSuccess
            prefs.getClearCode() -> {
                // Wipes the stored photos and their files, not just the database rows.
                photosRepository.clear()
                // And what the app remembers about the person: the passcode and the wipe code
                // itself. Leaving those behind meant the phone still showed that someone kept
                // something here under a code - which is exactly what this code exists to deny.
                prefs.clear()
                AuthPinContract.State.DataCleared
            }
            else -> AuthPinContract.State.AuthFailure
        }
    }
}
