package com.gps.zazor.ui.auth.pin

import androidx.lifecycle.viewModelScope
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface AuthPinViewModel : BaseViewModel<AuthPinContract.State, AuthPinContract.Event>

class AuthPinViewModelImpl(private val prefs: AppPreferences,
                           private val photosRepository: PhotoRepository) : BaseViewModelImpl<AuthPinContract.State, AuthPinContract.Event>(), AuthPinViewModel {

    override suspend fun initialState(): AuthPinContract.State? = null

    override fun onEventArrived(event: AuthPinContract.Event?) {
        when (event) {
            is AuthPinContract.Event.PinEntered -> {
                viewModelScope.launch(Dispatchers.IO) {
                    checkPin(event.pin)
                }
            }
        }
    }

    private fun checkPin(pin: String) {
        if (pin.length == PIN_LENGTH) {
            uiState.value = when (pin) {
                prefs.getPin() -> AuthPinContract.State.AuthSuccess
                prefs.getClearCode() -> {
                    photosRepository.clear()
                    AuthPinContract.State.DataCleared
                }
                else -> AuthPinContract.State.AuthFailure
            }
        }
    }
}