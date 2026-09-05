package com.gps.zazor.ui.media.edit

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.gps.zazor.R
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.databinding.FragmentEditPhotoBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.media.edit.di.injectViewModel
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_OFF_DISTANCE
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_ON_DISTANCE
import com.gps.zazor.ui.photo.editPhoto.DASH_PATH_PHASE
import com.gps.zazor.ui.photo.editPhoto.EditPhotoBottomSheet
import com.gps.zazor.ui.photo.editPhoto.SELECTOR_BUTTON_COLOR_DEFAULT
import com.gps.zazor.ui.photo.editPhoto.STROKE_WIDTH_FOR_DASH_LINE
import com.gps.zazor.utils.FragmentArgumentDelegate
import com.gps.zazor.utils.extensions.getBitmap
import com.gps.zazor.utils.extensions.show
import com.gps.zazor.utils.viewBinding.viewBinding
import com.gps.zazor.utils.audio.VoiceNoteRecorder
import com.gps.zazor.views.ShowButtonOnSelector

class EditMediaFragment : BaseFragment<EditMediaContract.State, EditMediaContract.Event>(R.layout.fragment_edit_photo) {

    companion object {

        fun newInstance(path: String) = EditMediaFragment().apply {
            this.photoPath = path
        }
    }

    override val viewModel by injectViewModel()

    private var photoPath by FragmentArgumentDelegate<String>()

    private val binding by viewBinding(FragmentEditPhotoBinding::bind)

    private val sheetBinding by viewBinding(BottomSheetAddNoteBinding::bind, R.id.clRoot)

    private val addNoteSheet by lazy { EditPhotoBottomSheet(sheetBinding) }

    private val recorder by lazy { VoiceNoteRecorder(requireContext()) }

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startRecording()
            else Toast.makeText(requireContext(), R.string.voice_note_denied, Toast.LENGTH_LONG).show()
        }

    override fun observeState(state: EditMediaContract.State?) {
        when (state) {
            is EditMediaContract.State.AddNotes -> binding.run {
                clPreviewContainer.show()
                dvNotes.elevation = 5F
                evDroidArt.elevation = 0F
                vDraw.elevation = 0F
                dvNotes.addNotes(state.notes, state.lat, state.long, state.date, state.time, state.accuracy)
            }
            is EditMediaContract.State.AddOverlay -> binding.run {
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
            is EditMediaContract.State.AllowDraw -> binding.run {
                dvNotes.elevation = 0F
                evDroidArt.elevation = 0F
                vDraw.elevation = 5F
                vDraw.isVisible = true
                vDraw.isPaintAllowed = true
                state.color?.let { vDraw.colorRes = it }
                vDraw.mode = state.mode
            }
            is EditMediaContract.State.DisallowDraw -> binding.vDraw.isPaintAllowed = false
            is EditMediaContract.State.SaveNotes -> binding.clPreviewContainer.getBitmap()?.let {
                viewModel.sendEvent(EditMediaContract.Event.SaveEdits(it))
            }
            is EditMediaContract.State.ClearDraw -> binding.vDraw.clear()
            is EditMediaContract.State.VoiceNote -> renderVoiceNote(state.path != null)
            is EditMediaContract.State.Done -> requireActivity().onBackPressedDispatcher.onBackPressed()
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOverlayEditor()
        // decodeFile returns null for a missing or corrupted file; leave the preview empty rather
        // than blowing up on a photo whose file was deleted outside the app.
        photoPath?.let { path ->
            BitmapFactory.decodeFile(path)?.let(binding.ivPreview::setImageBitmap)
            viewModel.sendEvent(EditMediaContract.Event.Initial(path))
        }
        binding.ivVoiceNote.setOnClickListener { toggleRecording() }
        addNoteSheet.show()
    }

    override fun onPause() {
        // Leaving mid-recording must not keep the microphone open or leave a stub file behind.
        if (recorder.isRecording) {
            recorder.cancel()
            showRecording(false)
        }
        super.onPause()
    }

    /**
     * Typing in the field is often impossible - gloves, rain, one hand busy - so the same remark
     * is spoken instead. The permission is requested here, on the tap, never at startup.
     */
    private fun toggleRecording() {
        when {
            recorder.isRecording -> stopRecording()
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> startRecording()
            else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        val started = recorder.start(onMaxDurationReached = { if (isAdded) stopRecording() })
        if (started) {
            showRecording(true)
            Toast.makeText(requireContext(), R.string.voice_note_recording, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.voice_note_empty, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        val file = recorder.stop()
        showRecording(false)
        if (file == null) {
            Toast.makeText(requireContext(), R.string.voice_note_empty, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.sendEvent(EditMediaContract.Event.SaveVoiceNote(file.absolutePath))
        Toast.makeText(requireContext(), R.string.voice_note_saved, Toast.LENGTH_SHORT).show()
    }

    /**
     * `isSelected` did nothing here - the icon is a plain drawable, not a state list - so the tint
     * is what actually shows that the microphone is live.
     */
    private fun showRecording(isRecording: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(),
            if (isRecording) R.color.ds_danger else R.color.ds_text_primary
        )
        // mutate() first: a drawable inflated from resources shares its ConstantState, so tinting
        // it directly would also repaint the same icon in the gallery rows.
        binding.ivVoiceNote.drawable?.mutate()?.setTint(color)
    }

    private fun renderVoiceNote(hasNote: Boolean) {
        showRecording(false)
        binding.ivVoiceNote.alpha = if (hasNote) 1F else 0.6F
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
}
