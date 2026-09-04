package com.gps.zazor.ui.photo.base

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentBasicPhotoBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.PhotoHandler
import com.gps.zazor.ui.photo.base.di.injectViewModel
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_OFF_DISTANCE
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_ON_DISTANCE
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_PHASE
import com.gps.zazor.ui.photo.editPhoto.SELECTOR_BUTTON_COLOR_DEFAULT
import com.gps.zazor.ui.photo.editPhoto.STROKE_WIDTH_FOR_DASH_LINE
import com.gps.zazor.utils.camera.CameraController
import com.gps.zazor.utils.location.SignalQuality
import com.gps.zazor.utils.extensions.getBitmap
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.show
import com.gps.zazor.utils.viewBinding.viewBinding
import com.gps.zazor.views.ShowButtonOnSelector

abstract class BasePhotoFragment :
    BaseFragment<BasePhotoContract.State, BasePhotoContract.Event>(R.layout.fragment_basic_photo),
    PhotoHandler {

    override val screenTitle = R.string.photo

    /** Screens that want the widest back lens override this; see [PanoramaFragment]. */
    protected open val useUltraWide: Boolean = false

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentBasicPhotoBinding::bind)

    private var camera: CameraController? = null

    /** True while the shutter request is in flight, so a double tap cannot queue two captures. */
    private var isCapturing = false

    private var lastSignal: SignalQuality? = null

    abstract fun onPhotoReady(bitmap: Bitmap)

    override fun observeState(state: BasePhotoContract.State?) {
        when (state) {
            is BasePhotoContract.State.FlipCamera -> camera?.flip(viewLifecycleOwner) { showCameraError() }
            is BasePhotoContract.State.ToggleFlash -> camera?.setTorch(state.isOn)
            is BasePhotoContract.State.AddNotes -> addNotes(state)
            is BasePhotoContract.State.AddOverlay -> binding.run {
                dvNotes.elevation = 0F
                evDroidArt.elevation = 5F
                vDraw.elevation = 0F
                if (evDroidArt.text != state.text) {
                    callback?.collapseEditPhoto()
                }
                evDroidArt.show()
                state.text?.let { evDroidArt.text = it }
                state.fontId?.let { evDroidArt.fontId = it }
                state.color?.let { evDroidArt.textColor = it }
            }
            is BasePhotoContract.State.AllowDraw -> binding.run {
                dvNotes.elevation = 0F
                evDroidArt.elevation = 0F
                vDraw.elevation = 5F
                vDraw.isVisible = true
                vDraw.isPaintAllowed = true
                state.color?.let { vDraw.colorRes = it }
                vDraw.mode = state.mode
            }
            is BasePhotoContract.State.DisallowDraw -> binding.vDraw.run {
                elevation = 0F
                isPaintAllowed = false
            }
            is BasePhotoContract.State.SaveNotes -> binding.clPreviewContainer.getBitmap()?.let(::onPhotoReady)
            is BasePhotoContract.State.ClearDraw -> binding.vDraw.clear()
            is BasePhotoContract.State.Initial -> binding.tvTrial.isVisible = state.isTrial
            is BasePhotoContract.State.ShowPreview -> showPreview(state)
            is BasePhotoContract.State.HidePreview -> {
                hidePreview()
                binding.tvTrial.isVisible = state.isTrial
            }
            is BasePhotoContract.State.Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        camera = CameraController(requireContext(), binding.vCamera)
        binding.run {
            ivFlash.setOnClickListener {
                viewModel.sendEvent(BasePhotoContract.Event.ToggleFlash)
            }
            ivBack.setOnClickListener {
                viewModel.sendEvent(BasePhotoContract.Event.BackPressed)
            }
            tvClearAll.setOnClickListener {
                callback?.clearAll()
            }
            ivSettings.setOnClickListener {
                callback?.openSettings()
            }
        }
        setupOverlayEditor()
        observeSignal()
    }

    /**
     * Shows how good the fix is, and warns before a shot that would carry a rough one.
     * Collected apart from the screen state so a position update cannot displace a capture.
     */
    private fun observeSignal() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signal.collect { quality ->
                    lastSignal = quality
                    binding.tvSignal.text = when {
                        !quality.hasFix -> getString(R.string.signal_waiting)
                        quality.isAcceptable ->
                            getString(R.string.signal_good, quality.accuracyMeters?.toInt() ?: 0)
                        else ->
                            getString(R.string.signal_weak, quality.accuracyMeters?.toInt() ?: 0)
                    }
                    binding.tvSignal.isVisible = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Do not restart the preview underneath the edit sheet.
        if (!binding.clPreviewContainer.isVisible) startCamera()
        callback?.onPhotoShown()
        viewModel.sendEvent(BasePhotoContract.Event.Resume)
    }

    override fun onPause() {
        super.onPause()
        camera?.stop()
        isCapturing = false
        viewModel.sendEvent(BasePhotoContract.Event.Pause)
    }

    override fun onStop() {
        super.onStop()
        viewModel.sendEvent(BasePhotoContract.Event.Stop)
    }

    override fun onDestroyView() {
        camera?.stop()
        camera = null
        super.onDestroyView()
    }

    /**
     * A rough fix means the stamped coordinate can be tens of metres out, which defeats the point
     * of the photo. Confirm before spending the shot rather than discovering it later.
     */
    override fun onCapturePhoto() {
        val quality = lastSignal
        if (quality?.shouldWarn == true) {
            confirmWeakSignal(quality)
        } else {
            capturePhoto()
        }
    }

    private fun confirmWeakSignal(quality: SignalQuality) {
        val message = quality.accuracyMeters
            ?.let { getString(R.string.signal_weak_message, it.toInt()) }
            ?: getString(R.string.signal_none_message)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.signal_weak_title)
            .setMessage(message)
            .setPositiveButton(R.string.signal_shoot_anyway) { _, _ -> capturePhoto() }
            .setNegativeButton(R.string.signal_wait, null)
            .show()
    }

    override fun flipCamera() {
        viewModel.sendEvent(BasePhotoContract.Event.FlipCamera)
    }

    private fun startCamera() {
        val controller = camera ?: return
        controller.start(viewLifecycleOwner) { showCameraError() }
        if (useUltraWide) {
            controller.setUltraWide(viewLifecycleOwner, wide = true) { showCameraError() }
        }
    }

    private fun addNotes(state: BasePhotoContract.State.AddNotes) {
        binding.run {
            clPreviewContainer.show()
            dvNotes.elevation = 5F
            evDroidArt.elevation = 0F
            vDraw.elevation = 0F
            dvNotes.addNotes(state.notes, state.lat, state.long, state.date, state.time, state.accuracy)
        }
    }

    private fun setupOverlayEditor() {
        with(binding.evDroidArt) {
            setPathEffectForSelector(
                DashPathEffect(
                    floatArrayOf(DASH_PATH_ON_DISTANCE, DASH_PATH_OFF_DISTANCE),
                    DASH_PATH_PHASE
                )
            )
            setStrokeWidthForDashLine(STROKE_WIDTH_FOR_DASH_LINE)
            setColorForTextShadow(Color.GRAY)
            setColorForSelectorButton(SELECTOR_BUTTON_COLOR_DEFAULT)
            setColorForDashLine(SELECTOR_BUTTON_COLOR_DEFAULT)
            showScaleRotateButton(ShowButtonOnSelector.HIDE_BUTTON)
            showResetViewTextButton(ShowButtonOnSelector.HIDE_BUTTON)
            showChangeViewTextButton(ShowButtonOnSelector.HIDE_BUTTON)
            setColorForSelector(Color.TRANSPARENT)
        }
    }

    private fun showPreview(state: BasePhotoContract.State.ShowPreview) {
        camera?.stop()
        callback?.onCaptured()
        with(binding) {
            vCamera.hide()
            toggleSettingsPanelVisibility(false)
            clPreviewContainer.show()
            ivPreview.show()
            ivPreview.setImageBitmap(state.bitmap)
        }
        addNotes(state.notes)
    }

    private fun hidePreview() {
        startCamera()
        callback?.onPhotoEditCancel()
        with(binding) {
            toggleSettingsPanelVisibility(true)
            vCamera.show()
            clPreviewContainer.hide()
            dvNotes.hide()
            evDroidArt.clear()
            evDroidArt.hide()
            vDraw.clear()
            vDraw.hide()
            ivPreview.setImageBitmap(null)
            ivPreview.hide()
        }
    }

    private fun toggleSettingsPanelVisibility(isVisible: Boolean) {
        with(binding) {
            ivBack.isVisible = !isVisible
            tvClearAll.isVisible = !isVisible
            ivFlash.isVisible = isVisible
            ivSettings.isVisible = isVisible
        }
    }

    private fun capturePhoto() {
        val controller = camera ?: return
        if (isCapturing || !controller.isRunning) return
        isCapturing = true
        controller.takePicture(
            onResult = { bitmap ->
                isCapturing = false
                if (isAdded) viewModel.sendEvent(BasePhotoContract.Event.PhotoCaptured(bitmap))
            },
            onError = {
                isCapturing = false
                showCameraError()
            }
        )
    }

    private fun showCameraError() {
        if (isAdded) {
            Toast.makeText(requireContext(), R.string.camera_error_msg, Toast.LENGTH_SHORT).show()
        }
    }
}
