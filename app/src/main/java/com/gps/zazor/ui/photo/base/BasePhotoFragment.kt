package com.gps.zazor.ui.photo.base

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.content.res.ColorStateList
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentBasicPhotoBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.PhotoHandler
import com.gps.zazor.ui.photo.SERIES_EXTRA_KEY
import com.gps.zazor.ui.photo.base.di.injectViewModel
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_OFF_DISTANCE
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_ON_DISTANCE
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_PHASE
import com.gps.zazor.ui.photo.editPhoto.SELECTOR_BUTTON_COLOR_DEFAULT
import com.gps.zazor.ui.photo.editPhoto.STROKE_WIDTH_FOR_DASH_LINE
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.utils.Formats
import com.gps.zazor.utils.PhotoComposer
import com.gps.zazor.utils.camera.CameraController
import com.gps.zazor.utils.location.SignalQuality
import com.gps.zazor.utils.extensions.getBitmap
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.show
import com.gps.zazor.utils.time.PhotoClock
import com.gps.zazor.utils.viewBinding.viewBinding
import org.koin.android.ext.android.inject
import com.gps.zazor.views.Mode
import com.gps.zazor.views.ShowButtonOnSelector

abstract class BasePhotoFragment :
    BaseFragment<BasePhotoContract.State, BasePhotoContract.Event>(R.layout.fragment_basic_photo),
    PhotoHandler {

    override val screenTitle = R.string.photo

    /** Screens that want the widest back lens override this; see [PanoramaFragment]. */
    /** The stamp preview must read exactly like the stamp, so it asks the same settings. */
    private val prefs: AppPreferences by inject()

    protected open val useUltraWide: Boolean = false

    /**
     * Whether this tab picks up a series handed over by the series screen.
     *
     * Only the ordinary photo tab does. Each tab keeps its own capture state and the pager builds
     * the neighbouring tabs as well as the open one, so without this the panorama tab could take
     * the series and the tab actually on screen would show none.
     */
    protected open val resumesSeriesFromIntent: Boolean = false

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentBasicPhotoBinding::bind)

    private var camera: CameraController? = null

    /** True while the shutter request is in flight, so a double tap cannot queue two captures. */
    private var isCapturing = false

    private var lastSignal: SignalQuality? = null

    /** Mirrored here so the menu can say "turn the flash off" rather than just "flash". */
    private var isFlashOn = false

    /** Whether the card has anything to say; it hides with the rest while a shot is on screen. */
    private var hasStampToShow = false

    /** True while a drawing tool is selected; then the corner button undoes instead of clearing. */
    private var isDrawing = false

    abstract fun onPhotoReady(bitmap: Bitmap)

    /**
     * What the corner button does, and says it does.
     *
     * While drawing it takes back the last mark; the rest of the time it wipes the edits. Those are
     * different enough that one label for both was a trap: a circle drawn slightly wrong left the
     * only button in sight, and pressing it threw away the note, the text and every other mark too.
     */
    private fun renderCornerAction() {
        binding.tvClearAll.setText(if (isDrawing) R.string.undo_last else R.string.clear_all)
    }

    /**
     * The picture that actually gets saved.
     *
     * The stamp and the marks are drawn onto the captured frame at its own resolution rather than
     * screenshotted off the preview, which used to hand the gallery a screen-sized copy with the
     * letterbox bars in it. Falls back to the old behaviour if there is no bitmap to compose onto.
     */
    private fun composeSavedPhoto(): Bitmap? = with(binding) {
        PhotoComposer.compose(ivPreview, listOf(dvNotes, evDroidArt, vDraw))
            ?: clPreviewContainer.getBitmap()
    }

    override fun observeState(state: BasePhotoContract.State?) {
        when (state) {
            is BasePhotoContract.State.FlipCamera -> camera?.flip(viewLifecycleOwner) { showCameraError() }
            is BasePhotoContract.State.ToggleFlash -> {
                isFlashOn = state.isOn
                camera?.setTorch(state.isOn)
            }
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
                vDraw.strokeWidth = state.width
                isDrawing = true
                renderCornerAction()
            }
            is BasePhotoContract.State.DisallowDraw -> binding.vDraw.run {
                elevation = 0F
                isPaintAllowed = false
                isDrawing = false
                renderCornerAction()
            }
            is BasePhotoContract.State.SaveNotes -> composeSavedPhoto()?.let(::onPhotoReady)
            is BasePhotoContract.State.ClearDraw -> binding.vDraw.clear()
            is BasePhotoContract.State.UndoDraw -> binding.vDraw.undo()
            is BasePhotoContract.State.ShowPreview -> showPreview(state)
            is BasePhotoContract.State.HidePreview -> hidePreview()
            is BasePhotoContract.State.Exit -> exitScreen()
            else -> Unit
        }
    }

    /** Leaving the screen for good. A collage cell closes itself instead - see its override. */
    protected open fun exitScreen() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        camera = CameraController(requireContext(), binding.vCamera)
        binding.vDraw.onMarkAdded = ::hideMarkerHint
        resumeSeriesFromIntent()
        binding.run {
            ivBack.setOnClickListener {
                viewModel.sendEvent(BasePhotoContract.Event.BackPressed)
            }
            tvClearAll.setOnClickListener {
                if (isDrawing) callback?.undoPaint() else callback?.clearAll()
            }
            ivMenu.setOnClickListener(::showCameraMenu)
        }
        applyStatusBarInset()
        setupOverlayEditor()
        observeSignal()
    }

    /**
     * The capture screen draws under the status bar. With only their layout margins the corner
     * controls sat on top of the clock and the status icons, so the bar's height is added to them.
     */
    private fun applyStatusBarInset() {
        val topControls =
            with(binding) { listOf(llSignal, ivMenu, ivBack, tvClearAll) }
        val layoutMargins = topControls.associateWith { it.marginTop }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            topControls.forEach { control ->
                control.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = layoutMargins.getValue(control) + statusBar
                }
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    /**
     * Shows how good the fix is, and warns before a shot that would carry a rough one.
     * Collected apart from the screen state so a position update cannot displace a capture.
     */
    private fun observeSignal() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.series.collect(::renderSeries)
                }
                launch {
                    viewModel.stamp.collect(::renderStampPreview)
                }
                viewModel.signal.collect { quality ->
                    lastSignal = quality
                    // The dot the design puts beside the reading: green for a fix worth using,
                    // the warning colour for one that is not.
                    binding.vSignalDot.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            if (quality.isAcceptable) R.color.ds_signal_good else R.color.ds_warn
                        )
                    )
                    binding.tvSignal.text = when {
                        !quality.hasFix -> getString(R.string.signal_waiting)
                        quality.isAcceptable ->
                            getString(R.string.signal_good, quality.accuracyMeters?.toInt() ?: 0)
                        else ->
                            getString(R.string.signal_weak, quality.accuracyMeters?.toInt() ?: 0)
                    }
                    binding.llSignal.isVisible = !binding.clPreviewContainer.isVisible
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

    /**
     * Shows how many frames the open series holds and how good its best fix is.
     * An open series with no frames yet must read differently from no series at all.
     */
    /**
     * The card over the viewfinder. Each line disappears when it has nothing to say - the stamp
     * will not print an address it does not have either, and an empty line on the card would be a
     * promise the photo does not keep.
     */
    private fun renderStampPreview(preview: StampPreview) {
        val format = prefs.getCoordinateFormat()
        binding.tvStampCoordinates.isVisible = preview.hasPosition
        if (preview.hasPosition) {
            binding.tvStampCoordinates.text =
                "${format.format(preview.lat!!)}, ${format.format(preview.lng!!)}"
        }
        binding.tvStampAddress.isVisible = !preview.address.isNullOrBlank()
        binding.tvStampAddress.text = preview.address.orEmpty()
        val now = PhotoClock.now()
        binding.tvStampDate.isVisible = prefs.isDisplayDate()
        binding.tvStampDate.text = PhotoClock.formatDate(now)
        binding.tvStampTime.isVisible = prefs.isDisplayTime()
        binding.tvStampTime.text = PhotoClock.formatTime(now)
        binding.tvStampAccuracy.isVisible = preview.accuracyMeters != null
        binding.tvStampAccuracy.text = preview.accuracyMeters
            ?.let { getString(R.string.accuracy_short, it.toInt()) }
            .orEmpty()
        hasStampToShow = preview.hasPosition ||
            !preview.address.isNullOrBlank() ||
            binding.tvStampDate.isVisible ||
            binding.tvStampTime.isVisible ||
            binding.tvStampAccuracy.isVisible
        // Nothing to show at all - no fix yet, everything switched off - is no card.
        binding.llStampPreview.isVisible = hasStampToShow && !binding.clPreviewContainer.isVisible
    }

    private fun frames(count: Int): String =
        resources.getQuantityString(R.plurals.series_frames_count, count, count)

    /**
     * An open series is a chip on the card rather than a control of its own: the design has no
     * second pill up there, and a person who has started a series still has to be able to see
     * that every shutter press is joining it.
     */
    private fun renderSeries(progress: SeriesProgress) {
        val best = progress.bestAccuracy?.toInt()
        binding.tvStampSeries.isVisible = progress.isOpen
        binding.tvStampSeries.text = when {
            !progress.isOpen -> ""
            best == null -> getString(R.string.series_active, frames(progress.frameCount))
            else -> getString(R.string.series_active_accuracy, frames(progress.frameCount), best)
        }
    }

    private fun startCamera() {
        val controller = camera ?: return
        controller.start(viewLifecycleOwner) { showCameraError() }
        if (useUltraWide) {
            controller.setUltraWide(viewLifecycleOwner, wide = true) { showCameraError() }
        }
    }

    /**
     * "One more frame" on the series screen comes back here with the series in the intent. The
     * extra is removed once used, so a later return to the camera does not silently reopen a
     * series the person has since closed.
     */
    private fun resumeSeriesFromIntent() {
        if (!resumesSeriesFromIntent) return
        val intent = requireActivity().intent ?: return
        val seriesId = intent.getStringExtra(SERIES_EXTRA_KEY) ?: return
        intent.removeExtra(SERIES_EXTRA_KEY)
        viewModel.sendEvent(BasePhotoContract.Event.ResumeSeries(seriesId))
    }

    private fun hideMarkerHint() {
        binding.tvMarkerHint.isVisible = false
    }

    private fun addNotes(state: BasePhotoContract.State.AddNotes) {
        binding.run {
            clPreviewContainer.show()
            dvNotes.elevation = 5F
            evDroidArt.elevation = 0F
            vDraw.elevation = 0F
            dvNotes.addNotes(
                state.notes, state.lat, state.long, state.date, state.time, state.accuracy,
                // Only on a wide frame: on an ordinary close-up the direction says little, and
                // the stamp is already four lines long.
                state.bearing?.takeIf { useUltraWide }?.let { Formats.bearing(requireContext(), it) },
                state.address
            )
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
        callback?.onCaptured(isWide = useUltraWide)
        with(binding) {
            vCamera.hide()
            toggleSettingsPanelVisibility(false)
            clPreviewContainer.show()
            ivPreview.show()
            ivPreview.setImageBitmap(state.bitmap)
            // After layout: the photo's rectangle is only known once the view has measured, and
            // the marks are pinned to the picture rather than to the screen around it.
            ivPreview.post {
                PhotoComposer.fitOverlays(ivPreview, listOf(dvNotes, evDroidArt, vDraw))
            }
        }
        addNotes(state.notes)
        // A panorama gets the design's review screen rather than the editing sheet, and opens
        // ready to be pinned: on a wide frame the one thing the receiver needs is which of the
        // things in it the photo is about.
        if (useUltraWide) showPanoramaReview(state.notes)
    }

    /**
     * The wide tab's review screen: the frame in a band, what it is about to carry underneath,
     * and the two buttons the mockup gives it.
     */
    private fun showPanoramaReview(notes: BasePhotoContract.State.AddNotes) = with(binding) {
        clPreviewContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = resources.getDimensionPixelSize(R.dimen.ds_panorama_band)
            topToTop = ConstraintLayout.LayoutParams.UNSET
            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            topToBottom = R.id.llPanoTitle
            bottomToTop = R.id.clPanoCard
            verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
        }
        tvPanoBearing.isVisible = notes.bearing != null
        notes.bearing?.let { tvPanoBearing.text = Formats.bearing(requireContext(), it) }
        // Each line goes when it has nothing to say, exactly as the stamp itself drops it.
        val hasPosition = notes.lat != null && notes.long != null
        ivPanoPin.isVisible = hasPosition
        tvPanoCoordinates.isVisible = hasPosition
        tvPanoCoordinates.text = "${notes.lat}, ${notes.long}"
        tvPanoAddress.isVisible = !notes.address.isNullOrBlank()
        tvPanoAddress.text = notes.address.orEmpty()
        tvPanoDate.isVisible = !notes.date.isNullOrBlank()
        tvPanoDate.text = notes.date.orEmpty()
        tvPanoTime.isVisible = !notes.time.isNullOrBlank()
        tvPanoTime.text = notes.time.orEmpty()
        val meters = notes.accuracy?.toIntOrNull()
        tvPanoAccuracy.isVisible = meters != null
        meters?.let { tvPanoAccuracy.text = getString(R.string.point_meta_accuracy, it) }
        setPanoramaReviewVisible(true)
        bPanoMark.setOnClickListener { armMarker() }
        bPanoSave.setOnClickListener { composeSavedPhoto()?.let(::onPhotoReady) }
        armMarker()
    }

    private fun setPanoramaReviewVisible(isVisible: Boolean) = with(binding) {
        llPanoTitle.isVisible = isVisible
        clPanoCard.isVisible = isVisible
        tvPanoHint.isVisible = isVisible
        bPanoMark.isVisible = isVisible
        bPanoSave.isVisible = isVisible
    }

    /**
     * The pin, armed without the editing sheet: the review screen has one button for it, and the
     * sheet it would otherwise come from is not on screen.
     */
    private fun armMarker() = with(binding) {
        dvNotes.elevation = 0F
        evDroidArt.elevation = 0F
        vDraw.elevation = 5F
        vDraw.isVisible = true
        vDraw.isPaintAllowed = true
        vDraw.mode = Mode.MARKER
        vDraw.colorRes = R.color.ds_danger
        isDrawing = true
    }

    /** Puts the frame back across the whole screen, for the tabs that fill it. */
    private fun restorePreviewToFullScreen() {
        binding.clPreviewContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = ConstraintLayout.LayoutParams.MATCH_PARENT
            topToBottom = ConstraintLayout.LayoutParams.UNSET
            bottomToTop = ConstraintLayout.LayoutParams.UNSET
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    }

    private fun hidePreview() {
        setPanoramaReviewVisible(false)
        restorePreviewToFullScreen()
        startCamera()
        callback?.onPhotoEditCancel()
        // The next shot starts with nothing drawn, so the button starts as Clear all again.
        isDrawing = false
        renderCornerAction()
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
            ivMenu.isVisible = isVisible
            llSignal.isVisible = isVisible && lastSignal != null
            llStampPreview.isVisible = isVisible && hasStampToShow
        }
    }

    /**
     * Everything the camera screen can do besides taking the picture.
     *
     * The design puts one button here, not a row of them: over a live frame each extra control is
     * a piece of the photograph you cannot see. The flash and the series say their current state
     * in the item itself, so the menu reads as a status as well as a set of actions.
     */
    private fun showCameraMenu(anchor: View) {
        val series = viewModel.series.value
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, MENU_FLASH, 0, getString(
                if (isFlashOn) R.string.flash_off else R.string.flash_on
            ))
            menu.add(0, MENU_SERIES, 1, if (series.isOpen) {
                getString(R.string.series_close, frames(series.frameCount))
            } else {
                getString(R.string.series_start)
            })
            menu.add(0, MENU_SETTINGS, 2, getString(R.string.settings))
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_FLASH -> viewModel.sendEvent(BasePhotoContract.Event.ToggleFlash)
                    MENU_SERIES -> viewModel.sendEvent(BasePhotoContract.Event.ToggleSeries)
                    MENU_SETTINGS -> callback?.openSettings()
                }
                true
            }
        }.show()
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

/** Menu items on the camera: everything the screen does besides taking the picture. */
private const val MENU_FLASH = 1
private const val MENU_SERIES = 2
private const val MENU_SETTINGS = 3
