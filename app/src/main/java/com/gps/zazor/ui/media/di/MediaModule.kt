package com.gps.zazor.ui.media.di

import com.gps.zazor.ui.media.MediaActivity
import com.gps.zazor.ui.media.MediaViewModel
import com.gps.zazor.ui.media.MediaViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val mediaModule = module {
    viewModel { MediaViewModelImpl() }
}

fun MediaActivity.injectViewModel(): Lazy<MediaViewModel> =
    lazy { getViewModel<MediaViewModelImpl>(null) }