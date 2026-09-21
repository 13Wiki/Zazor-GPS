package com.gps.zazor.ui.settings.list

import android.content.Context
import com.gps.zazor.R
import com.gps.zazor.ads.AdSlot
import com.gps.zazor.data.models.MainSettingType
import com.gps.zazor.data.models.SettingRow
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface SettingsListViewModel :
    BaseViewModel<SettingsListContract.State, SettingsListContract.Event>

class SettingsListViewModelImpl(
    private val context: Context,
    private val appPrefs: AppPreferences,
    private val adSlot: AdSlot
) : BaseViewModelImpl<SettingsListContract.State, SettingsListContract.Event>(),
    SettingsListViewModel {

    /**
     * Rebuilt on every open, so every row shows what it is set to right now.
     *
     * Grouped the way the screen is used rather than the way the code is laid out: what ends up on
     * the picture, what keeps other people out of it, and everything else.
     */
    private fun rows(): List<SettingRow> = listOfNotNull(
        SettingRow.Header(R.string.settings_group_capture),
        SettingRow.Item(
            type = MainSettingType.NOTES,
            titleRes = R.string.notes_setting,
            iconRes = R.drawable.ic_row_stamp,
            value = stampSummary()
        ),
        SettingRow.Item(
            type = MainSettingType.COORDINATE_FORMAT,
            titleRes = R.string.coordinate_format_setting,
            iconRes = R.drawable.ic_row_coordinates,
            value = appPrefs.getCoordinateFormat().sample
        ),

        SettingRow.Header(R.string.settings_group_protection),
        SettingRow.Item(
            type = MainSettingType.PIN_CODE,
            titleRes = R.string.pin_code_setting,
            iconRes = R.drawable.ic_row_lock,
            isChecked = appPrefs.hasPin()
        ),
        SettingRow.Item(
            type = MainSettingType.CLEAR_CODE,
            titleRes = R.string.clear_code_setting,
            iconRes = R.drawable.ic_row_wipe,
            isChecked = appPrefs.hasClearCode()
        ),
        SettingRow.Item(
            type = MainSettingType.APPEARANCE,
            titleRes = R.string.appearance_title,
            iconRes = R.drawable.ic_row_disguise,
            subtitleRes = R.string.appearance_row_subtitle
        ),
        SettingRow.Item(
            type = MainSettingType.PRIVACY,
            titleRes = R.string.privacy_setting,
            iconRes = R.drawable.ic_row_privacy,
            subtitleRes = R.string.privacy_row_subtitle
        ),

        SettingRow.Header(R.string.settings_group_other),
        // Nothing to remove while this build shows no ads: offering the purchase anyway is a row
        // that takes money for a change the person would not see. It returns with the ads.
        SettingRow.Item(
            type = MainSettingType.PRO,
            titleRes = R.string.pro_setting,
            iconRes = R.drawable.ic_row_pro,
            isChecked = appPrefs.isPro()
        ).takeIf { adSlot.isAvailable },
        SettingRow.Item(
            type = MainSettingType.FEEDBACK,
            titleRes = R.string.feedback_setting,
            iconRes = R.drawable.ic_row_feedback
        )
    )

    /** How much of the stamp is switched on, without making anyone open the screen to count. */
    private fun stampSummary(): String {
        val fields = listOf(
            appPrefs.isDisplayCoordinates(),
            appPrefs.isDisplayDate(),
            appPrefs.isDisplayTime(),
            appPrefs.isDisplayAccuracy(),
            appPrefs.isDisplayAddress()
        )
        return context.getString(R.string.stamp_fields_summary, fields.count { it }, fields.size)
    }

    override suspend fun initialState(): SettingsListContract.State =
        SettingsListContract.State.Initial(rows())

    override fun onEventArrived(event: SettingsListContract.Event?) = Unit
}
