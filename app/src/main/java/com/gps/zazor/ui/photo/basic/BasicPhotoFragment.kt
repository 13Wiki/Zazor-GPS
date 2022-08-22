package com.gps.zazor.ui.photo.basic

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.gps.zazor.ui.photo.base.BasePhotoContract
import com.gps.zazor.ui.photo.base.BasePhotoFragment
import com.gps.zazor.utils.FragmentArgumentDelegate

class BasicPhotoFragment : BasePhotoFragment() {

    override fun onPhotoReady(bitmap: Bitmap) {
        viewModel.sendEvent(BasePhotoContract.Event.SaveEdits(bitmap))
    }
}