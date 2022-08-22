package com.gps.zazor.ui.settings.pin.di

import com.gps.zazor.ui.settings.pin.PinCodeSetupFragment
import com.gps.zazor.ui.settings.pin.PinCodeSetupViewModel
import com.gps.zazor.ui.settings.pin.PinCodeSetupViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val pinCodeSetupModule = module {
    viewModel { PinCodeSetupViewModelImpl(get()) }
}

fun PinCodeSetupFragment.injectViewModel(): Lazy<PinCodeSetupViewModel> =
    lazy { getViewModel<PinCodeSetupViewModelImpl>(null) }