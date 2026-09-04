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
import com.gps.zazor.ui.settings.appearance.AppearanceFragment
import com.gps.zazor.ui.privacy.PrivacyFragment
import com.gps.zazor.billing.PlayProStatus
import com.gps.zazor.billing.ProStatus
import android.widget.Toast
import org.koin.android.ext.android.inject

class SettingsActivity : BaseActivity<SettingsContract.State, SettingsContract.Event>(R.layout.activity_settings),
    SettingsCallback {

    companion object {

        fun newIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
    }

    override val viewModel by injectViewModel()

    private val proStatus: ProStatus by inject()

    override fun observeState(state: SettingsContract.State?) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateTo(SettingsListFragment(), R.id.flContainer)
        proStatus.refresh()
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

    override fun openAppearance() {
        navigateTo(AppearanceFragment(), R.id.flContainer)
    }

    override fun openPrivacy() {
        navigateTo(PrivacyFragment.newInstance(isFirstRun = false), R.id.flContainer)
    }

    /**
     * Opens the store flow. Until the product exists in Play Console there is nothing to buy, so
     * the person is told plainly rather than left tapping a dead button.
     */
    override fun openPro() {
        val status = proStatus as? PlayProStatus
        if (status == null || status.priceLabel.value == null) {
            Toast.makeText(this, R.string.pro_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        status.purchase(this)
    }
}