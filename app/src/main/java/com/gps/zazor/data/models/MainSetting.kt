package com.gps.zazor.data.models

import androidx.annotation.StringRes

enum class MainSettingType {
    PIN_CODE, NOTES, CLEAR_CODE, APPEARANCE, PRO, PRIVACY, FEEDBACK
}

data class MainSetting(val type: MainSettingType,
                       @StringRes
                       val stringRes: Int,
                       val isChecked: Boolean? = null)
