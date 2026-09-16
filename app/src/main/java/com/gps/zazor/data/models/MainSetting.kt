package com.gps.zazor.data.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class MainSettingType {
    PIN_CODE, NOTES, CLEAR_CODE, APPEARANCE, PRO, PRIVACY, FEEDBACK, COORDINATE_FORMAT
}

/**
 * A line of the settings screen: either a caption over a group, or a setting.
 *
 * The screen used to be one flat list of titles, which said nothing about what a setting currently
 * is - whether the passcode is on, how many fields the stamp carries, which coordinate format is
 * chosen. A row now carries its own answer, so the list can be read without opening anything.
 */
sealed class SettingRow {

    data class Header(@StringRes val titleRes: Int) : SettingRow()

    /**
     * @param value what the setting is set to right now, shown at the end of the row.
     * @param isChecked non-null turns the row into a switch instead of a value and a chevron.
     */
    data class Item(
        val type: MainSettingType,
        @StringRes val titleRes: Int,
        @DrawableRes val iconRes: Int,
        @StringRes val subtitleRes: Int? = null,
        val value: String? = null,
        val isChecked: Boolean? = null
    ) : SettingRow()
}
