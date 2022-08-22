package com.gps.zazor.ui.media.edit

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.cleveroad.droidart.ShowButtonOnSelector
import com.gps.zazor.R
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.databinding.FragmentEditPhotoBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.media.edit.di.injectViewModel
import com.gps.zazor.ui.photo.editPhoto.*
import com.gps.zazor.utils.FragmentArgumentDelegate
import com.gps.zazor.utils.extensions.*
import com.gps.zazor.utils.viewBinding.viewBinding
import kotlinx.android.synthetic.main.fragment_basic_photo.*

class EditMediaFragment : BaseFragment<EditMediaContract.State, EditMediaContract.Event>(R.layout.fragment_edit_photo) {

    companion object {

        fun newInstance(path: String) = EditMediaFragment().apply {
            this.photoPath = path
        }
    }

    override val viewModel by injectViewModel()

    private var photoPath by FragmentArgumentDelegate<String>()

    private val binding by viewBinding(FragmentEditPhotoBinding::bind)

    private val sheetBinding by viewBinding(BottomSheetAddNoteBinding::bind) {
        it.requireView().findViewById(R.id.clRoot)
    }

    private val addNoteSheet by lazy {
        EditPhotoBottomSheet(sheetBinding)
    }

    override fun observeState(state: EditMediaContract.State?) {
        when (state) {
            is EditMediaContract.State.AddNotes -> {
                binding.run {
                    clPreviewContainer.show()
                    dvNotes.elevation = 5F
                    evDroidArt.elevation = 0F
                    vDraw.elevation = 0F
                    dvNotes.addNotes(state.notes, state.lat, state.long, state.date, state.time, state.accuracy)
                }
            }
            is EditMediaContract.State.AddOverlay -> {
                binding.run {
                    dvNotes.elevation = 0F
                    evDroidArt.elevation = 5F
                    vDraw.elevation = 0F
                }
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
            is EditMediaContract.State.AllowDraw -> {
                binding.run {
                    dvNotes.elevation = 0F
                    evDroidArt.elevation = 0F
                    vDraw.elevation = 5F
                }
                binding.vDraw.run {
                    isVisible = true
                    isPaintAllowed = true
                    state.color?.let {
                        colorRes = it
                    }
                    mode = state.mode
                }
            }
            is EditMediaContract.State.DisallowDraw -> {
                binding.vDraw.isPaintAllowed = false
            }
            is EditMediaContract.State.SaveNotes -> {
                clPreviewContainer.getBitmap()?.let {
                    viewModel.sendEvent(EditMediaContract.Event.SaveEdits(it))
                }
            }
            is EditMediaContract.State.ClearDraw -> {
                binding.vDraw.clear()
            }
            is EditMediaContract.State.Done -> {
                activity?.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.sendEvent(EditMediaContract.Event.Initial(photoPath!!))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDroidArt()
        binding.ivPreview.setImageBitmap(BitmapFactory.decodeFile(photoPath!!))
        addNoteSheet.show()
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
}