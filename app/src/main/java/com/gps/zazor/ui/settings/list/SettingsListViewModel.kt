package com.gps.zazor.ui.settings.list

import com.gps.zazor.R
import com.gps.zazor.data.models.MainSetting
import com.gps.zazor.data.models.MainSettingType
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface SettingsListViewModel :
    BaseViewModel<SettingsListContract.State, SettingsListContract.Event>

class SettingsListViewModelImpl(appPrefs: AppPreferences) :
    BaseViewModelImpl<SettingsListContract.State, SettingsListContract.Event>(),
    SettingsListViewModel {

    private val settings = listOf(
        MainSetting(MainSettingType.PIN_CODE, R.string.pin_code_setting, appPrefs.getPin() != null),
        MainSetting(MainSettingType.NOTES, R.string.notes_setting),
        MainSetting(MainSettingType.CLEAR_CODE, R.string.clear_code_setting, appPrefs.getClearCode() != null),
        MainSetting(MainSettingType.TRIAL_CODE, R.string.use_trial_code)
    )

    override suspend fun initialState(): SettingsListContract.State =
        SettingsListContract.State.Initial(settings)

    override fun onEventArrived(event: SettingsListContract.Event?) = Unit
}