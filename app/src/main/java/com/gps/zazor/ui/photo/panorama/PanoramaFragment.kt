package com.gps.zazor.ui.photo.panorama

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.isVisible
import com.dermandar.dmd_lib.DMD_Capture
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentPanoramaBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.PhotoHandler
import com.gps.zazor.ui.photo.panorama.di.injectViewModel
import com.gps.zazor.utils.PhotoStorage
import com.gps.zazor.utils.SimpleShooterCallback
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.viewBinding.viewBinding
import java.io.File

class PanoramaFragment : BaseFragment<PanoramaContract.State, PanoramaContract.Event>(R.layout.fragment_panorama),
    PhotoHandler {

    companion object {

        private const val PREVIEW_WIDTH = 400
        private const val PREVIEW_HEIGHT = 500
        private const val HIDDEN_GL_VIEW = "com.dermandar.dmd_lib.YinYangGLView"
    }

    override val viewModel by injectViewModel()

    override val screenTitle = R.string.panorama

    private val binding by viewBinding(FragmentPanoramaBinding::bind)

    private val shooterCallback = object : SimpleShooterCallback() {

        override fun shootingCompleted(finished: Boolean) {
            isShooting = false
        }
    }

    private var dmdCapture: DMD_Capture? = null

    private var isShooting = false

    override fun observeState(state: PanoramaContract.State?) {
        when (state) {
            is PanoramaContract.State.Saved ->
                Toast.makeText(requireContext(), R.string.collage_added, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCapture()
    }

    override fun onResume() {
        super.onResume()
        callback?.onPanoramaShown()
        // The library is a Camera1 wrapper; on devices where it cannot open the camera it throws
        // instead of returning, which used to take the whole app down when this tab was swiped to.
        runCatching { dmdCapture?.startCamera(requireContext(), PREVIEW_WIDTH, PREVIEW_HEIGHT) }
            .onFailure { showUnavailable() }
    }

    override fun onPause() {
        super.onPause()
        // Release the camera so the photo and collage tabs can open it.
        runCatching {
            if (isShooting) dmdCapture?.stopShooting()
            isShooting = false
            dmdCapture?.stopCamera()
        }
    }

    override fun onDestroyView() {
        runCatching { dmdCapture?.releaseShooter() }
        dmdCapture = null
        super.onDestroyView()
    }

    /** The shared shutter button starts the sweep and, on the second tap, finishes it. */
    override fun onCapturePhoto() {
        val capture = dmdCapture ?: return
        runCatching {
            if (isShooting) {
                capture.finishShooting()
                isShooting = false
            } else {
                isShooting = capture.startShooting(panoramaDirectory().absolutePath)
            }
        }.onFailure {
            isShooting = false
            showUnavailable()
        }
    }

    override fun flipCamera() = Unit

    private fun initCapture() {
        val capture = runCatching { DMD_Capture() }.getOrElse {
            showUnavailable()
            return
        }
        val rotation = runCatching { currentRotation() }.getOrDefault(0)

        val cameraView = runCatching {
            capture.setPhotoSavedCallback { path ->
                path?.let { viewModel.sendEvent(PanoramaContract.Event.PanoramaSaved(it)) }
            }
            capture.initShooter(requireContext(), shooterCallback, rotation, false, false)
        }.getOrElse {
            showUnavailable()
            return
        }

        dmdCapture = capture
        binding.flContainer.addView(cameraView)
        binding.flContainer.post {
            (binding.flContainer.children.firstOrNull() as? ViewGroup)?.children?.forEach {
                if (it.javaClass.name == HIDDEN_GL_VIEW) it.hide()
            }
        }
    }

    /** `Context.getDisplay()` only exists from API 30; fall back to the window manager below it. */
    @Suppress("DEPRECATION")
    private fun currentRotation(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireContext().display?.rotation ?: 0
        } else {
            requireActivity().windowManager.defaultDisplay.rotation
        }

    private fun panoramaDirectory(): File =
        File(
            requireContext().getExternalFilesDir(PhotoStorage.DIR_PHOTOS)
                ?: requireContext().filesDir,
            "panorama_" + System.currentTimeMillis()
        ).also { it.mkdirs() }

    private fun showUnavailable() {
        if (!isAdded) return
        binding.flContainer.isVisible = false
        Toast.makeText(requireContext(), R.string.panorama_unavailable, Toast.LENGTH_LONG).show()
    }
}
