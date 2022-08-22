package com.gps.zazor.ui.settings.clearCode

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val clearCodeSetupModule = module {
    viewModel { ClearCodeSetupViewModelImpl(get()) }
}

fun ClearCodeSetupFragment.injectViewModel(): Lazy<ClearCodeSetupViewModelImpl> =
    lazy { getViewModel<ClearCodeSetupViewModelImpl>(null) }