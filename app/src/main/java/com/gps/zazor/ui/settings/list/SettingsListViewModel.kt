package com.gps.zazor.ui.settings.list

import com.gps.zazor.R
import com.gps.zazor.ads.AdSlot
import com.gps.zazor.data.models.MainSetting
import com.gps.zazor.data.models.MainSettingType
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface SettingsListViewModel :
    BaseViewModel<SettingsListContract.State, SettingsListContract.Event>

class SettingsListViewModelImpl(
    private val appPrefs: AppPreferences,
    private val adSlot: AdSlot
) : BaseViewModelImpl<SettingsListContract.State, SettingsListContract.Event>(),
    SettingsListViewModel {

    /** Rebuilt on every open so the checkmarks reflect what is actually set right now. */
    private fun settings() = listOfNotNull(
        MainSetting(MainSettingType.PIN_CODE, R.string.pin_code_setting, appPrefs.hasPin()),
        MainSetting(MainSettingType.NOTES, R.string.notes_setting),
        MainSetting(MainSettingType.CLEAR_CODE, R.string.clear_code_setting, appPrefs.hasClearCode()),
        MainSetting(MainSettingType.APPEARANCE, R.string.appearance_title),
        MainSetting(MainSettingType.PRIVACY, R.string.privacy_setting),
        // Nothing to remove while this build shows no ads: offering the purchase anyway is a row
        // that takes money for a change the person would not see. It returns with the ads.
        MainSetting(MainSettingType.PRO, R.string.pro_setting, appPrefs.isPro())
            .takeIf { adSlot.isAvailable },
        MainSetting(MainSettingType.TRIAL_CODE, R.string.use_trial_code)
    )

    override suspend fun initialState(): SettingsListContract.State =
        SettingsListContract.State.Initial(settings())

    override fun onEventArrived(event: SettingsListContract.Event?) = Unit
}