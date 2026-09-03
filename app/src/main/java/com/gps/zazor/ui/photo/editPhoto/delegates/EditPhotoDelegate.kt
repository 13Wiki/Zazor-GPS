package com.gps.zazor.ui.photo.editPhoto.delegates

import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModel
import com.gps.zazor.utils.extensions.findActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class EditPhotoDelegate(
    protected val sheetBinding: BottomSheetAddNoteBinding,
    protected val viewModel: EditPhotoViewModel
) {

    private var stateJob: Job? = null

    abstract fun observeState(state: EditPhotoContract.State?)

    abstract fun clear()

    /**
     * Subscribes once. The context of a sheet view is a themed wrapper rather than the activity,
     * so it has to be unwrapped before a lifecycle scope can be taken from it.
     */
    @CallSuper
    open fun onShown() {
        if (stateJob?.isActive == true) return
        val owner = sheetBinding.root.context.findActivity() ?: return
        stateJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::observeState)
            }
        }
    }

    protected fun getString(@StringRes res: Int): String =
        sheetBinding.root.context.getString(res)
}
