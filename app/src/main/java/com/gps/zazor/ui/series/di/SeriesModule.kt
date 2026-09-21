package com.gps.zazor.ui.series.di

import com.gps.zazor.ui.series.SeriesFragment
import com.gps.zazor.ui.series.SeriesViewModel
import com.gps.zazor.ui.series.SeriesViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val seriesModule = module {
    viewModel { SeriesViewModelImpl(get()) }
}

fun SeriesFragment.injectViewModel(): Lazy<SeriesViewModel> =
    lazy { getViewModel<SeriesViewModelImpl>() }
