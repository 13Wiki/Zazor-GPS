package com.gps.zazor.ui.auth

import com.gps.zazor.R
import com.gps.zazor.ui.auth.di.injectViewModel
import com.gps.zazor.ui.auth.pin.AuthPinFragment
import com.gps.zazor.ui.base.BaseActivity
import com.gps.zazor.ui.photo.PhotoActivity

class AuthActivity : BaseActivity<AuthContract.State, AuthContract.Event>(R.layout.activity_auth) {

    override val viewModel by injectViewModel()

    override fun observeState(state: AuthContract.State?) {
        when (state) {
            is AuthContract.State.Initial -> {
                if (state.needAuth){
                    navigateTo(AuthPinFragment(), R.id.flContainer)
                }
                else {
                    startActivity(PhotoActivity.newIntent(this))
                    finish()
                }
            }
        }
    }
}