package com.gps.zazor.ui.privacy

import com.gps.zazor.analytics.Analytics
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface PrivacyViewModel : BaseViewModel<PrivacyContract.State, PrivacyContract.Event>

/**
 * The first-run screen. For an app whose claim is that positions never leave the phone, this is
 * the argument, not a formality: it states plainly what is never collected, and the counters that
 * are can be refused right here.
 */
class PrivacyViewModelImpl(
    private val prefs: AppPreferences,
    private val analytics: Analytics
) : BaseViewModelImpl<PrivacyContract.State, PrivacyContract.Event>(), PrivacyViewModel {

    override suspend fun initialState(): PrivacyContract.State =
        PrivacyContract.State.Content(prefs.isAnalyticsEnabled())

    override fun onEventArrived(event: PrivacyContract.Event?) {
        when (event) {
            is PrivacyContract.Event.ToggleAnalytics -> {
                analytics.setEnabled(event.enabled)
                uiState.value = PrivacyContract.State.Content(event.enabled)
            }
            is PrivacyContract.Event.Accept -> {
                prefs.setPrivacyAccepted(true)
                // Nothing was sent while the choice was still on screen; start honouring it now.
                // The open is counted once, by the launch screen; counting it here too would
                // double every install's first session.
                analytics.setEnabled(prefs.isAnalyticsEnabled())
                uiState.value = PrivacyContract.State.Accepted
            }
            else -> Unit
        }
    }
}
