package com.gps.zazor.ui.photo.collage.photo

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.gps.zazor.ui.photo.base.BasePhotoContract
import com.gps.zazor.ui.photo.base.BasePhotoFragment
import com.gps.zazor.ui.photo.basic.BasicPhotoFragment
import com.gps.zazor.ui.photo.collage.photo.di.injectViewModel
import com.gps.zazor.utils.FragmentArgumentDelegate

class CollagePhotoFragment : BasePhotoFragment() {

    companion object {

        fun newInstance(collageIndex: Int) = CollagePhotoFragment().apply {
            this.collageIndex = collageIndex
        }
    }

    private var collageIndex by FragmentArgumentDelegate<Int>()

    override val viewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.sendEvent(BasePhotoContract.Event.CollageInitial(collageIndex!!))
    }

    override fun onPhotoReady(bitmap: Bitmap) {
        viewModel.sendEvent(BasePhotoContract.Event.SaveEdits(bitmap))
    }
}