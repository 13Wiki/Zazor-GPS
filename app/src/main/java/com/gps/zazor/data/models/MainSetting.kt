package com.gps.zazor.data.models

import androidx.annotation.StringRes

enum class MainSettingType {
    PIN_CODE, NOTES, CLEAR_CODE, TRIAL_CODE
}

data class MainSetting(val type: MainSettingType,
                       @StringRes
                       val stringRes: Int,
                       val isChecked: Boolean? = null)