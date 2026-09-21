package com.gps.zazor.ui.settings.support.di

import com.gps.zazor.ui.settings.support.SupportFragment
import com.gps.zazor.ui.settings.support.SupportViewModel
import com.gps.zazor.ui.settings.support.SupportViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val supportModule = module {
    viewModel { SupportViewModelImpl() }
}

fun SupportFragment.injectViewModel(): Lazy<SupportViewModel> =
    lazy { getViewModel<SupportViewModelImpl>() }
