package com.gps.zazor.ui.photo.base.di

import com.gps.zazor.ui.photo.base.BasePhotoFragment
import com.gps.zazor.ui.photo.base.BasePhotoViewModel
import com.gps.zazor.ui.photo.base.BasePhotoViewModelImpl
import com.gps.zazor.ui.photo.di.ADD_NOTE_FLOW
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module


val basicPhotoModule = module {

    viewModel {
        BasePhotoViewModelImpl(
            androidApplication(),
            get(named(ADD_NOTE_FLOW)),
            get(),
            get()
        )
    }
}

fun BasePhotoFragment.injectViewModel(): Lazy<BasePhotoViewModel> =
    lazy { getViewModel<BasePhotoViewModelImpl>(null) }