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
    /**
     * Marks where each row sits inside its group, so the screen can draw a group as one block
     * with hairlines in it rather than as a stack of separate cards.
     */
    private fun grouped(rows: List<SettingRow>): List<SettingRow> = rows.mapIndexed { index, row ->
        if (row !is SettingRow.Item) return@mapIndexed row
        val first = rows.getOrNull(index - 1) !is SettingRow.Item
        val last = rows.getOrNull(index + 1) !is SettingRow.Item
        row.copy(
            place = when {
                first && last -> SettingRow.Place.ONLY
                first -> SettingRow.Place.FIRST
                last -> SettingRow.Place.LAST
                else -> SettingRow.Place.MIDDLE
            }
        )
    }

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
            value = context.getString(appPrefs.getCoordinateFormat().titleRes)
        ),
        // A switch in the list, as the design has it: this one is a yes or no, and opening a
        // screen to flip it would be a screen with one line on it.
        SettingRow.Item(
            type = MainSettingType.WAIT_FIX,
            titleRes = R.string.setting_wait_fix,
            iconRes = R.drawable.ic_row_coordinates,
            subtitle = context.getString(
                R.string.setting_wait_fix_subtitle,
                appPrefs.getAccuracyThresholdMeters()
            ),
            isChecked = appPrefs.isWaitForAccurateFix()
        ),

        SettingRow.Header(R.string.settings_group_protection),
        // The passcode says whether it is on and opens the screen that sets it; the wipe code
        // lives on the disguise screen, which is where the design keeps it.
        SettingRow.Item(
            type = MainSettingType.PIN_CODE,
            titleRes = R.string.pin_code_setting,
            iconRes = R.drawable.ic_row_lock,
            value = context.getString(
                if (appPrefs.hasPin()) R.string.setting_on else R.string.setting_off
            ),
            isValueGood = appPrefs.hasPin()
        ),
        SettingRow.Item(
            type = MainSettingType.APPEARANCE,
            titleRes = R.string.appearance_row_title,
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

    override fun onEventArrived(event: SettingsListContract.Event?) {
        when (event) {
            is SettingsListContract.Event.ToggleWaitForFix -> {
                appPrefs.putWaitForAccurateFix(!appPrefs.isWaitForAccurateFix())
                uiState.value = SettingsListContract.State.Initial(grouped(rows()))
            }
            else -> Unit
        }
    }

    override suspend fun initialState(): SettingsListContract.State =
        SettingsListContract.State.Initial(grouped(rows()))

}
