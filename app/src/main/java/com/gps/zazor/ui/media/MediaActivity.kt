package com.gps.zazor.ui.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gps.zazor.R
import com.gps.zazor.ui.base.BaseActivity
import com.gps.zazor.ui.media.di.injectViewModel
import com.gps.zazor.ui.media.edit.EditMediaFragment
import com.gps.zazor.ui.media.list.MediaListFragment
import com.gps.zazor.ui.outings.OutingsFragment

class MediaActivity : BaseActivity<MediaContract.State, MediaContract.Event>(R.layout.activity_media), MediaCallback {

    companion object {

        fun newIntent(context: Context) = Intent(context, MediaActivity::class.java)
    }

    override val viewModel by injectViewModel()

    override fun observeState(state: MediaContract.State?) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateTo(MediaListFragment(), R.id.flContainer)
    }

    override fun editPhoto(photoPath: String) {
        navigateTo(EditMediaFragment.newInstance(photoPath), R.id.flContainer, true)
    }

    override fun openOutings() {
        navigateTo(OutingsFragment(), R.id.flContainer, true)
    }
}