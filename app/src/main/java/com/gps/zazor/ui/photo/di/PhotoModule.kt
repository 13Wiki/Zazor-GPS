package com.gps.zazor.ui.photo.di

import com.gps.zazor.ui.photo.PhotoActivity
import com.gps.zazor.ui.photo.PhotoViewModel
import com.gps.zazor.ui.photo.PhotoViewModelImpl
import com.gps.zazor.ui.photo.editPhoto.EditPhotoBottomSheet
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModel
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModelImpl
import com.gps.zazor.utils.extensions.findActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val ADD_NOTE_FLOW = "add_note_flow"

val photoModule = module {

    // Buffered so an edit action emitted before the photo screen re-subscribes is not lost.
    single(named(ADD_NOTE_FLOW)) {
        MutableSharedFlow<EditPhotoContract.Flow>(extraBufferCapacity = 16)
    }

    viewModel { PhotoViewModelImpl(get()) }

    viewModel { EditPhotoViewModelImpl(get(named(ADD_NOTE_FLOW)), get()) }
}

fun PhotoActivity.injectViewModel(): Lazy<PhotoViewModel> =
    lazy { getViewModel<PhotoViewModelImpl>() }

fun EditPhotoBottomSheet.injectViewModel(): Lazy<EditPhotoViewModel> =
    lazy {
        // The sheet's context is often a ContextThemeWrapper, so the old `as ComponentActivity`
        // cast threw a NullPointerException instead of finding the host.
        val activity = requireNotNull(binding.root.context.findActivity()) {
            "EditPhotoBottomSheet must be hosted by a ComponentActivity"
        }
        activity.getViewModel<EditPhotoViewModelImpl>()
    }
