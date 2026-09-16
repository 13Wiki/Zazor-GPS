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
        uiState.value = when {
            prefs.isPin(pin) -> AuthPinContract.State.AuthSuccess
            prefs.isClearCode(pin) -> {
                // Wipes the stored photos and their files, not just the database rows.
                photosRepository.clear()
                // And what the app remembers about the person: the passcode and the wipe code
                // itself. Leaving those behind meant the phone still showed that someone kept
                // something here under a code - which is exactly what this code exists to deny.
                //
                // The paid entitlement is not evidence of anything and belongs to the Google
                // account rather than to this phone, so it is carried across the wipe. Play would
                // restore it on the next query anyway; keeping it here means no ads in between.
                val wasPro = prefs.isPro()
                prefs.clear()
                if (wasPro) prefs.setPro(true)
                AuthPinContract.State.DataCleared
            }
            else -> AuthPinContract.State.AuthFailure
        }
    }
}
