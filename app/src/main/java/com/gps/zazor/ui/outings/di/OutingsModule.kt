package com.gps.zazor.ui.outings.di

import com.gps.zazor.ui.outings.OutingsFragment
import com.gps.zazor.ui.outings.OutingsViewModel
import com.gps.zazor.ui.outings.OutingsViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val outingsModule = module {
    viewModel { OutingsViewModelImpl(get(), get()) }
}

fun OutingsFragment.injectViewModel(): Lazy<OutingsViewModel> =
    lazy { getViewModel<OutingsViewModelImpl>() }
