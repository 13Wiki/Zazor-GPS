package com.gps.zazor.ui.photo.collage.base

import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.isVisible
import com.gps.zazor.R
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.PhotoHandler
import com.gps.zazor.ui.photo.collage.di.injectViewModel
import com.gps.zazor.utils.extensions.*
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.selector.continuousFocusPicture
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.view.CameraView

abstract class CollageFragment(layoutRes: Int) : BaseFragment<CollageContract.State, CollageContract.Event>(layoutRes),
    PhotoHandler {

    abstract val previewViews: List<ImageView>

    abstract val captureViews: List<ImageView>

    abstract val collagePreview: View

    override val viewModel by injectViewModel()

    override fun observeState(state: CollageContract.State?) {
        when (state) {
            is CollageContract.State.ShowPreview -> {
                previewViews[state.index].run {
                    show()
                    setImageBitmap(state.bitmap)
                }
                viewModel.sendEvent(CollageContract.Event.PreviewShown)
            }
            is CollageContract.State.CaptureCollage -> {
                captureViews.forEach { it.isVisible = false }
                collagePreview.getBitmap()?.let {
                    viewModel.sendEvent(CollageContract.Event.SaveEdits(it))
                }
                Toast.makeText(requireContext(), R.string.collage_added, Toast.LENGTH_LONG).show()
                resetPreview()
            }
            is CollageContract.State.AllowCollageCapture -> {
                callback?.switchEnabledCapture(true)
            }
            is CollageContract.State.DisallowCollageCapture -> {
                callback?.switchEnabledCapture(false)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.sendEvent(CollageContract.Event.Initial(previewViews.size))
        captureViews.forEachIndexed { index, captureView ->
            captureView.setOnClickListener {
                callback?.openCollagePhoto(index)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.sendEvent(CollageContract.Event.Resume)
    }

    override fun onPause() {
        super.onPause()
        callback?.switchEnabledCapture(true)
    }

    override fun flipCamera() = Unit

    override fun onCapturePhoto() {
        viewModel.sendEvent(CollageContract.Event.CapturePressed)
    }

    private fun resetPreview() {
        callback?.switchEnabledCapture(false)
        previewViews.forEach {
            it.setImageBitmap(null)
        }
        captureViews.forEach {
            it.isVisible = true
        }
    }
}