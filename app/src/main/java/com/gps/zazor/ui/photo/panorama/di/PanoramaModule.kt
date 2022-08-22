package com.gps.zazor.ui.photo.panorama.di

import com.gps.zazor.ui.photo.panorama.PanoramaFragment
import com.gps.zazor.ui.photo.panorama.PanoramaViewModel
import com.gps.zazor.ui.photo.panorama.PanoramaViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val panoramaModule = module {

    viewModel {
        PanoramaViewModelImpl()
    }
}

fun PanoramaFragment.injectViewModel(): Lazy<PanoramaViewModel> =
    lazy { getViewModel<PanoramaViewModelImpl>(null) }