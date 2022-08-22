package com.gps.zazor.ui.auth

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface AuthViewModel : BaseViewModel<AuthContract.State, AuthContract.Event>

class AuthViewModelImpl(private val prefs: AppPreferences) : BaseViewModelImpl<AuthContract.State, AuthContract.Event>(), AuthViewModel {

    override suspend fun initialState(): AuthContract.State = AuthContract.State.Initial(prefs.getPin() != null)

    override fun onEventArrived(event: AuthContract.Event?) = Unit
}