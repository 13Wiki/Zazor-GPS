package com.gps.zazor.ui.auth

import com.gps.zazor.R
import com.gps.zazor.ui.auth.di.injectViewModel
import com.gps.zazor.ui.auth.pin.AuthPinFragment
import com.gps.zazor.ui.base.BaseActivity
import com.gps.zazor.ui.photo.PhotoActivity
import com.gps.zazor.ui.privacy.PrivacyFragment

class AuthActivity : BaseActivity<AuthContract.State, AuthContract.Event>(R.layout.activity_auth),
    PrivacyFragment.Host {

    override val viewModel by injectViewModel()

    override fun observeState(state: AuthContract.State?) {
        when (state) {
            is AuthContract.State.NeedsPrivacy -> showPrivacy()
            is AuthContract.State.Initial -> {
                if (state.needAuth) {
                    navigateTo(AuthPinFragment(), R.id.flContainer)
                } else {
                    startActivity(PhotoActivity.newIntent(this))
                    finish()
                }
            }
            else -> Unit
        }
    }

    override fun onPrivacyAccepted() {
        viewModel.sendEvent(AuthContract.Event.Recheck)
    }

    private fun showPrivacy() {
        if (supportFragmentManager.findFragmentByTag(TAG_PRIVACY) != null) return
        supportFragmentManager.beginTransaction()
            .replace(R.id.flContainer, PrivacyFragment.newInstance(isFirstRun = true), TAG_PRIVACY)
            .commit()
    }

    private companion object {

        const val TAG_PRIVACY = "privacy"
    }
}
