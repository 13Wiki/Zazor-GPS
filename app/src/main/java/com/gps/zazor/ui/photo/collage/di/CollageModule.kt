package com.gps.zazor.ui.photo.collage.di

import com.gps.zazor.ui.photo.collage.base.CollageFragment
import com.gps.zazor.ui.photo.collage.base.CollageViewModel
import com.gps.zazor.ui.photo.collage.base.CollageViewModelImpl
import com.gps.zazor.ui.photo.collage.photo.di.COLLAGE_PHOTO_FLOW
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val collageModule = module {

    viewModel {
        CollageViewModelImpl(androidApplication(), get(named(COLLAGE_PHOTO_FLOW)), get())
    }
}

fun CollageFragment.injectViewModel(): Lazy<CollageViewModel> =
    lazy { getViewModel<CollageViewModelImpl>(null) }