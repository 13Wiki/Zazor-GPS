package com.gps.zazor.ui.photo.editPhoto.delegates

import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class EditPhotoDelegate(protected val sheetBinding : BottomSheetAddNoteBinding,
                                 protected val viewModel: EditPhotoViewModel) {

    abstract fun observeState(state: EditPhotoContract.State?)

    abstract fun clear()

    @CallSuper
    open fun onShown() {
        (sheetBinding.root.context as? LifecycleOwner)?.lifecycleScope?.launch {
            viewModel.uiState.collect(::observeState)
        }
    }

    protected fun getString(@StringRes res: Int) =
        sheetBinding.root.context.getString(res)
}