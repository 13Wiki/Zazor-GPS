package com.gps.zazor.ui.settings

import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface SettingsViewModel : BaseViewModel<SettingsContract.State, SettingsContract.Event>

class SettingsViewModelImpl : BaseViewModelImpl<SettingsContract.State, SettingsContract.Event>(), SettingsViewModel {

    override suspend fun initialState(): SettingsContract.State = SettingsContract.State.Initial

    override fun onEventArrived(event: SettingsContract.Event?) = Unit
}