package com.gps.zazor.ui.settings.clearCode

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.settings.pin.PinCodeSetupViewModelImpl

class ClearCodeSetupViewModelImpl(appPreferences: AppPreferences) : PinCodeSetupViewModelImpl(appPreferences) {

    override fun setCode(code: String?) {
        prefs.putClearCode(code)
    }

    override fun getCode(): String? = prefs.getClearCode()
}