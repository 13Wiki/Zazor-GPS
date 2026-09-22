package com.gps.zazor.data.models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gps.zazor.R

enum class MainSettingType {
    PIN_CODE, NOTES, CLEAR_CODE, APPEARANCE, PRO, PRIVACY, FEEDBACK, COORDINATE_FORMAT, WAIT_FIX
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
        /** Blue for everything the app does, red for the one row that can take things away. */
        @ColorRes val iconTintRes: Int = R.color.ds_row_icon,
        @StringRes val subtitleRes: Int? = null,
        /** A subtitle that carries a number, so it cannot be a bare string resource. */
        val subtitle: String? = null,
        val value: String? = null,
        /** Draws the value in the good-signal green: the design marks a protection that is on. */
        val isValueGood: Boolean = false,
        val isChecked: Boolean? = null,
        /**
         * Where the row sits in its group. The design draws a group as one block with hairlines
         * inside it, so a row has to know whether it is an end of that block or the middle of it.
         */
        val place: Place = Place.ONLY
    ) : SettingRow()

    enum class Place { FIRST, MIDDLE, LAST, ONLY }
}
