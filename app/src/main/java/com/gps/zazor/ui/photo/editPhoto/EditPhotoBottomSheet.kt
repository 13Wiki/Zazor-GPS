package com.gps.zazor.ui.photo.editPhoto

import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.ui.base.BasePersistentBottomSheet
import com.gps.zazor.ui.photo.PhotoHandler
import com.gps.zazor.ui.photo.di.injectViewModel
import com.gps.zazor.ui.photo.editPhoto.delegates.AddNoteDelegate
import com.gps.zazor.ui.photo.editPhoto.delegates.AddOverlayDelegate
import com.gps.zazor.ui.photo.editPhoto.delegates.AddPaintDelegate
import com.gps.zazor.ui.photo.editPhoto.delegates.EditPhotoDelegate
import com.gps.zazor.utils.extensions.toggle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.gps.zazor.views.Mode
import java.lang.ref.WeakReference

class EditPhotoBottomSheet(val binding: BottomSheetAddNoteBinding)
    : BasePersistentBottomSheet<EditPhotoContract.State, EditPhotoContract.Event>(){

    override val viewModel by injectViewModel()

    override val view = binding.root

    override val behavior = BottomSheetBehavior.from(binding.clRoot)

    private val delegates = mutableListOf<EditPhotoDelegate>()

    override fun observeState(state: EditPhotoContract.State?) {
        when  (state) {
            EditPhotoContract.State.NotesScreen -> selectTab(binding.tvTabNote)
            is EditPhotoContract.State.PaintScreen -> selectTab(binding.tvTabPaint)
            is EditPhotoContract.State.TextScreen -> selectTab(binding.tvTabText)
            is EditPhotoContract.State.ShowOverlay -> {
                behavior.state = STATE_COLLAPSED
            }
            else -> Unit
        }
    }

    override fun onShown() {
        initDelegates()
        super.onShown()
        behavior.peekHeight = PEEK_HEIGHT
        with(binding) {
            tvTabNote.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.NotesTabPressed)
            }
            tvTabPaint.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.LINE))
            }
            tvTabText.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.TextTabPressed)
            }
            tvCancel.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.CancelPressed)
            }
            tvDone.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.DonePressed)
            }
        }
    }

    fun clearAll() {
        delegates.forEach {
            it.clear()
        }
    }

    /** One mark back, rather than everything at once - see the drawing button on the frame. */
    /** One mode is chosen at a time, and the pill says which. */
    private fun selectTab(tab: android.widget.TextView) {
        listOf(binding.tvTabNote, binding.tvTabPaint, binding.tvTabText).forEach {
            it.isSelected = it === tab
        }
    }

    /** What a panorama opens on: the drawing tab, with the pin already chosen. */
    fun startMarkerMode() {
        initDelegates()
        viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.MARKER))
    }

    fun undoPaint() {
        viewModel.sendEvent(EditPhotoContract.Event.UndoPaint)
    }

    /** Built once; the old version appended three more delegates on every `show()`. */
    private fun initDelegates() {
        if (delegates.isNotEmpty()) return
        delegates.add(AddNoteDelegate(binding, viewModel))
        delegates.add(AddOverlayDelegate(binding, viewModel))
        delegates.add(AddPaintDelegate(binding, viewModel))
        delegates.forEach { it.onShown() }
    }
}

private const val PEEK_HEIGHT = 120
