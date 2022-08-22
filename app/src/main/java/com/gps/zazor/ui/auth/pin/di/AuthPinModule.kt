package com.gps.zazor.ui.auth.pin.di

import com.gps.zazor.ui.auth.pin.AuthPinFragment
import com.gps.zazor.ui.auth.pin.AuthPinViewModel
import com.gps.zazor.ui.auth.pin.AuthPinViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val authPinModule = module {
    viewModel { AuthPinViewModelImpl(get(), get()) }
}

fun AuthPinFragment.injectViewModel(): Lazy<AuthPinViewModel> =
    lazy { getViewModel<AuthPinViewModelImpl>(null) }