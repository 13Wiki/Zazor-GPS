package com.gps.zazor.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.gps.zazor.BuildConfig
import com.gps.zazor.R
import com.gps.zazor.data.models.CoordinateFormat
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.ui.base.BaseActivity
import com.gps.zazor.ui.settings.clearCode.ClearCodeSetupFragment
import com.gps.zazor.ui.settings.di.injectViewModel
import com.gps.zazor.ui.settings.list.SettingsListFragment
import com.gps.zazor.ui.settings.notes.NotesSettingsFragment
import com.gps.zazor.ui.settings.pin.PinCodeSetupFragment
import com.gps.zazor.ui.settings.support.SupportFragment
import com.gps.zazor.ui.settings.appearance.AppearanceFragment
import com.gps.zazor.ui.privacy.PrivacyFragment
import com.gps.zazor.billing.PlayProStatus
import com.gps.zazor.billing.ProStatus
import org.koin.android.ext.android.inject

class SettingsActivity : BaseActivity<SettingsContract.State, SettingsContract.Event>(R.layout.activity_settings),
    SettingsCallback {

    companion object {

        fun newIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
    }

    override val viewModel by injectViewModel()

    private val proStatus: ProStatus by inject()

    private val prefs: AppPreferences by inject()

    override fun observeState(state: SettingsContract.State?) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showList()
        proStatus.refresh()
    }

    /*
     * Every sub-screen goes on the back stack. Without it, back from any of them left settings
     * altogether and dropped the person onto the camera, so changing two settings in a row meant
     * walking into settings twice.
     */

    override fun openPinSetup() {
        navigateTo(PinCodeSetupFragment(), R.id.flContainer, addToBackStack = true)
    }

    override fun openClearCodeSetup() {
        navigateTo(ClearCodeSetupFragment(), R.id.flContainer, addToBackStack = true)
    }

    override fun openNotesSettings() {
        navigateTo(NotesSettingsFragment(), R.id.flContainer, addToBackStack = true)
    }

    /**
     * Decimal degrees or degrees-minutes-seconds.
     *
     * A choice between two ways of writing the same point is a question, not a screen: it is asked
     * where it was tapped and answered in one press. The list is rebuilt afterwards because the row
     * carries the answer.
     */
    override fun openCoordinateFormat() {
        val formats = CoordinateFormat.values()
        val labels = formats.map { getString(it.titleRes) }.toTypedArray<CharSequence>()
        AlertDialog.Builder(this)
            .setTitle(R.string.coordinate_format_setting)
            .setSingleChoiceItems(labels, formats.indexOf(prefs.getCoordinateFormat())) { dialog, which ->
                prefs.putCoordinateFormat(formats[which])
                dialog.dismiss()
                showList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun openAppearance() {
        navigateTo(AppearanceFragment(), R.id.flContainer, addToBackStack = true)
    }

    override fun openPrivacy() {
        navigateTo(PrivacyFragment.newInstance(isFirstRun = false), R.id.flContainer, addToBackStack = true)
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

    /**
     * A letter to the author, written where the thing went wrong.
     *
     * The first people to use this are the ones who will say what is broken and what it should do
     * instead, and an address buried in a store listing is not where that gets written. The form
     * asks what it is about and what happened, then hands the letter to the person's own mail app
     * - the app has no server to send it to, and nothing goes anywhere until they press send.
     */
    override fun openFeedback() {
        navigateTo(SupportFragment(), R.id.flContainer, addToBackStack = true)
    }

    private fun showList() {
        navigateTo(SettingsListFragment(), R.id.flContainer)
    }
}
