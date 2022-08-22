package com.gps.zazor.ui.settings.clearCode

import com.gps.zazor.R
import com.gps.zazor.ui.settings.pin.PinCodeSetupFragment

class ClearCodeSetupFragment: PinCodeSetupFragment() {

    override val errorRes = R.string.code_incorrect

    override val titleRes: Int = R.string.enter_clear_code

    override val viewModel by injectViewModel()
}