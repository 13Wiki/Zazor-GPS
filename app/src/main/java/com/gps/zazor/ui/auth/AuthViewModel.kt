package com.gps.zazor.ui.auth

import com.gps.zazor.analytics.Analytics
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface AuthViewModel : BaseViewModel<AuthContract.State, AuthContract.Event>

class AuthViewModelImpl(
    private val prefs: AppPreferences,
    private val analytics: Analytics
) : BaseViewModelImpl<AuthContract.State, AuthContract.Event>(), AuthViewModel {

    override suspend fun initialState(): AuthContract.State = decide()

    override fun onEventArrived(event: AuthContract.Event?) {
        when (event) {
            is AuthContract.Event.Recheck -> uiState.value = decide()
            else -> Unit
        }
    }

    /**
     * The privacy screen comes before everything, including the PIN: the person should know what
     * the app collects before they invest anything in it.
     */
    private fun decide(): AuthContract.State =
        if (!prefs.isPrivacyAccepted()) {
            AuthContract.State.NeedsPrivacy
        } else {
            analytics.track(Analytics.Event.APP_OPENED)
            AuthContract.State.Initial(prefs.getPin() != null)
        }
}
