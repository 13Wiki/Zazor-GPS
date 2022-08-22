package com.gps.zazor.ui.settings.trial

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface TrialCodeViewModel :
    BaseViewModel<TrialCodeContract.State, TrialCodeContract.Event>

class TrialCodeViewModelImpl(private val prefs: AppPreferences) : BaseViewModelImpl<TrialCodeContract.State, TrialCodeContract.Event>(), TrialCodeViewModel {

    override suspend fun initialState(): TrialCodeContract.State? = null

    override fun onEventArrived(event: TrialCodeContract.Event?) {
        when (event) {
            is TrialCodeContract.Event.CodeEntered -> checkCode(event.code)
        }
    }

    private fun checkCode(code: String) {
        val isValid = code == TRIAL_CODE
        uiState.value = if (isValid) {
            prefs.setTrial(false)
            TrialCodeContract.State.CodeSet
        } else {
            TrialCodeContract.State.CodeIncorrect
        }
    }
}