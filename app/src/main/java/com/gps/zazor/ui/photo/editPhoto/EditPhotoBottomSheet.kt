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
            EditPhotoContract.State.NotesScreen -> {
                binding.ivNote.toggle(true)
                binding.ivPaint.toggle(false)
                binding.ivText.toggle(false)
            }
            is EditPhotoContract.State.PaintScreen -> {
                binding.ivNote.toggle(false)
                binding.ivPaint.toggle(true)
                binding.ivText.toggle(false)
            }
            is EditPhotoContract.State.TextScreen -> {
                binding.ivNote.toggle(false)
                binding.ivPaint.toggle(false)
                binding.ivText.toggle(true)
            }
            is EditPhotoContract.State.ShowOverlay -> {
                behavior.state = STATE_COLLAPSED
            }
        }
    }

    override fun onShown() {
        initDelegates()
        super.onShown()
        behavior.peekHeight = 120
        with(binding) {
            ivNote.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event
                    .NotesTabPressed)
            }
            ivPaint.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.LINE))
            }
            ivText.setOnClickListener {
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

    private fun initDelegates() {
        delegates.add(AddNoteDelegate(binding, viewModel))
        delegates.add(AddOverlayDelegate(binding, viewModel))
        delegates.add(AddPaintDelegate(binding, viewModel))
        delegates.forEach {
            it.onShown()
        }
    }
}