package com.gps.zazor.ui.photo.collage.photo.di

import com.gps.zazor.ui.photo.base.BasePhotoFragment
import com.gps.zazor.ui.photo.base.BasePhotoViewModel
import com.gps.zazor.ui.photo.base.BasePhotoViewModelImpl
import com.gps.zazor.ui.photo.collage.photo.CollagePhoto
import com.gps.zazor.ui.photo.collage.photo.CollagePhotoFragment
import com.gps.zazor.ui.photo.collage.photo.CollagePhotoViewModelImpl
import com.gps.zazor.ui.photo.di.ADD_NOTE_FLOW
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val COLLAGE_PHOTO_FLOW = "collagePhoto"

val collagePhotoModule = module {

    single(named(COLLAGE_PHOTO_FLOW)) {
        MutableStateFlow<CollagePhoto?>(null)
    }

    viewModel {
        CollagePhotoViewModelImpl(
            androidApplication(),
            get(named(COLLAGE_PHOTO_FLOW)),
            get(named(ADD_NOTE_FLOW)),
            get(),
            get()
        )
    }
}

fun CollagePhotoFragment.injectViewModel(): Lazy<BasePhotoViewModel> =
    lazy { getViewModel<CollagePhotoViewModelImpl>(null) }