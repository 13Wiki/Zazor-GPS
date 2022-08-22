package com.gps.zazor.ui.photo.di

import androidx.activity.ComponentActivity
import com.gps.zazor.ui.photo.PhotoActivity
import com.gps.zazor.ui.photo.PhotoViewModel
import com.gps.zazor.ui.photo.PhotoViewModelImpl
import com.gps.zazor.ui.photo.editPhoto.EditPhotoBottomSheet
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModel
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModelImpl
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val ADD_NOTE_FLOW = "add_note_flow"

val photoModule = module {

    single(named(ADD_NOTE_FLOW)) { MutableSharedFlow<EditPhotoContract.Flow>() }

    viewModel { PhotoViewModelImpl(get<MutableSharedFlow<EditPhotoContract.Flow>>(named(ADD_NOTE_FLOW)), get()) }

    viewModel { EditPhotoViewModelImpl(get(named(ADD_NOTE_FLOW)), get()) }
}

fun PhotoActivity.injectViewModel(): Lazy<PhotoViewModel> =
      lazy { getViewModel<PhotoViewModelImpl>(null) }

fun EditPhotoBottomSheet.injectViewModel(): Lazy<EditPhotoViewModel> =
    lazy { (binding.root.context as? ComponentActivity)!!.getViewModel<EditPhotoViewModelImpl>() }