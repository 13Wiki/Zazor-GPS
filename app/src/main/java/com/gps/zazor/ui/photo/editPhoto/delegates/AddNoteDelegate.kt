package com.gps.zazor.ui.photo.editPhoto.delegates

import androidx.core.widget.doOnTextChanged
import com.gps.zazor.R
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.databinding.IncludeNotesContentBinding
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModel
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.show

class AddNoteDelegate(sheetBinding: BottomSheetAddNoteBinding,
                      viewModel: EditPhotoViewModel) : EditPhotoDelegate(sheetBinding, viewModel) {

    private val binding = IncludeNotesContentBinding.bind(sheetBinding.clRoot)

    override fun observeState(state: EditPhotoContract.State?) {
        when (state) {
            is EditPhotoContract.State.NotesScreen -> {
                binding.tilNote.show()
                sheetBinding.tvTitle.text = getString(R.string.notes)
            }
            else -> binding.tilNote.hide()
        }
    }

    override fun onShown() {
        super.onShown()
        binding.etNote.doOnTextChanged { text, _, _, _ ->
            viewModel.sendEvent(EditPhotoContract.Event.NotesEntered(text.toString()))
        }
    }

    override fun clear() {
        binding.etNote.setText("")
    }
}