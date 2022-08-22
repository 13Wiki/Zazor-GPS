package com.gps.zazor.ui.photo.base

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.DashPathEffect
import android.hardware.Camera
import android.hardware.camera2.CameraDevice
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.cleveroad.droidart.ShowButtonOnSelector
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentBasicPhotoBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.PhotoHandler
import com.gps.zazor.ui.photo.base.di.injectViewModel
import com.gps.zazor.ui.photo.editPhoto.*
import com.gps.zazor.utils.extensions.*
import com.gps.zazor.utils.viewBinding.viewBinding
import com.gps.zazor.views.Mode
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.selector.*
import kotlinx.android.synthetic.main.fragment_basic_photo.*

abstract class BasePhotoFragment :
    BaseFragment<BasePhotoContract.State, BasePhotoContract.Event>(R.layout.fragment_basic_photo),
    PhotoHandler {

    override val screenTitle = R.string.photo

    override val viewModel by injectViewModel()

    private var camera: Fotoapparat? = null

    private val binding by viewBinding(FragmentBasicPhotoBinding::bind)

    abstract fun onPhotoReady(bitmap: Bitmap)

    override fun observeState(state: BasePhotoContract.State?) {
        when (state) {
            is BasePhotoContract.State.FlipCamera -> state.configuration.run {
                camera?.switchTo(lensPosition = lensPosition, cameraConfiguration = configuration)
            }
            is BasePhotoContract.State.ToggleFlash -> camera?.updateConfiguration(
                state.config
            )
            is BasePhotoContract.State.AddNotes -> addNotes(state)
            is BasePhotoContract.State.AddOverlay -> {
                dvNotes.elevation = 0F
                evDroidArt.elevation = 5F
                vDraw.elevation = 0F
                if (binding.evDroidArt.text != state.text) {
                    callback?.collapseEditPhoto()
                }
                binding.evDroidArt.run {
                    show()
                    state.text?.let {
                        text = it
                    }
                    state.fontId?.let {
                        fontId = it
                    }
                    state.color?.let {
                        textColor = it
                    }
                }
            }
            is BasePhotoContract.State.AllowDraw -> {
                dvNotes.elevation = 0F
                evDroidArt.elevation = 0F
                vDraw.elevation = 5F
                binding.vDraw.run {
                    isVisible = true
                    isPaintAllowed = true
                    state.color?.let {
                        colorRes = it
                    }
                    mode = state.mode
                }
            }
            is BasePhotoContract.State.DisallowDraw -> {
                binding.vDraw.run {
                    elevation = 0F
                    isPaintAllowed = false
                }
            }
            is BasePhotoContract.State.SaveNotes -> {
                clPreviewContainer.getBitmap()?.let(::onPhotoReady)
            }
            is BasePhotoContract.State.ClearDraw -> {
                binding.vDraw.clear()
            }
            is BasePhotoContract.State.Initial -> {
                binding.tvTrial.isVisible = state.isTrial
            }
            is BasePhotoContract.State.ShowPreview -> showPreview(state)
            is BasePhotoContract.State.HidePreview -> {
                hidePreview()
                binding.tvTrial.isVisible = state.isTrial
            }
            is BasePhotoContract.State.Exit -> activity?.onBackPressed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        setupDroidArt()
    }

    override fun onResume() {
        super.onResume()
        setupCamera()
        callback?.onPhotoShown()
        viewModel.sendEvent(BasePhotoContract.Event.Resume)
    }

    override fun onPause() {
        super.onPause()
        camera?.stop()
        camera = null
        viewModel.sendEvent(BasePhotoContract.Event.Pause)
    }

    override fun onStop() {
        super.onStop()
        viewModel.sendEvent(BasePhotoContract.Event.Stop)
    }

    override fun onCapturePhoto() {
        capturePhoto()
    }

    override fun flipCamera() {
        viewModel.sendEvent(BasePhotoContract.Event.FlipCamera)
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

    private fun setupCamera() {
        camera = Fotoapparat(
            context = requireContext(),
            focusView = binding.focusView,
            cameraConfiguration = CameraConfiguration(pictureResolution = highestResolution()),
            view = binding.vCamera,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back()
        )
        camera?.start()
        // turn off camera sound
        (camera?.getPrivateField("device")
            ?.callMethod("getSelectedCamera")
            ?.getPrivateField("camera") as? Camera)?.enableShutterSound(false)
    }

    private fun setupDroidArt() {
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
            ivPreview.run {
                show()
                setImageBitmap(state.photo.bitmap)
            }
        }
        addNotes(state.notes)
    }

    private fun hidePreview() {
        setupCamera()
        callback?.onPhotoEditCancel()
        with(binding) {
            toggleSettingsPanelVisibility(true)
            vCamera.show()
            clPreviewContainer.hide()
            dvNotes.hide()
            evDroidArt.hide()
            vDraw.hide()
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
        camera?.run {
            autoFocus().takePicture()
        }?.let { photoResult ->
            photoResult
                .toBitmap()
                .whenAvailable { photo ->
                    photo?.let {
                        camera?.updateConfiguration(CameraConfiguration(focusMode = continuousFocusPicture()))
                        viewModel.sendEvent(BasePhotoContract.Event.PhotoCaptured(it))
                    }
                }
        }
    }
}