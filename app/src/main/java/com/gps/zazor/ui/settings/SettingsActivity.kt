package com.gps.zazor.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gps.zazor.R
import com.gps.zazor.ui.base.BaseActivity
import com.gps.zazor.ui.settings.clearCode.ClearCodeSetupFragment
import com.gps.zazor.ui.settings.di.injectViewModel
import com.gps.zazor.ui.settings.list.SettingsListFragment
import com.gps.zazor.ui.settings.notes.NotesSettingsFragment
import com.gps.zazor.ui.settings.pin.PinCodeSetupFragment
import com.gps.zazor.ui.settings.trial.TrialCodeFragment

class SettingsActivity : BaseActivity<SettingsContract.State, SettingsContract.Event>(R.layout.activity_settings),
    SettingsCallback {

    companion object {

        fun newIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
    }

    override val viewModel by injectViewModel()

    override fun observeState(state: SettingsContract.State?) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateTo(SettingsListFragment(), R.id.flContainer)
    }

    override fun openPinSetup() {
        navigateTo(PinCodeSetupFragment(), R.id.flContainer)
    }

    override fun openClearCodeSetup() {
        navigateTo(ClearCodeSetupFragment(), R.id.flContainer)
    }

    override fun openNotesSettings() {
        navigateTo(NotesSettingsFragment(), R.id.flContainer)
    }

    override fun openTrialCode() {
        navigateTo(TrialCodeFragment(), R.id.flContainer)
    }
}