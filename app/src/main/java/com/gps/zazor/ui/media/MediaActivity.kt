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
import com.gps.zazor.ui.photo.PhotoActivity
import com.gps.zazor.ui.series.SeriesFragment
import com.gps.zazor.ui.share.ShareFragment

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

    override fun openShare(paths: List<String>) {
        navigateTo(ShareFragment.newInstance(paths), R.id.flContainer, true)
    }

    override fun openSeries(seriesId: String) {
        navigateTo(SeriesFragment.newInstance(seriesId), R.id.flContainer, true)
    }

    /**
     * Finishes rather than stacking another camera on top: the camera is where this screen was
     * opened from, and coming back to it with the series open is what "one more frame" means.
     */
    override fun addToSeries(seriesId: String) {
        startActivity(PhotoActivity.newIntent(this, seriesId))
        finish()
    }
}