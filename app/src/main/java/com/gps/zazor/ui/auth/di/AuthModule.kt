package com.gps.zazor.ui.auth.di

import com.gps.zazor.ui.auth.AuthActivity
import com.gps.zazor.ui.auth.AuthViewModel
import com.gps.zazor.ui.auth.AuthViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val authModule = module {
    viewModel { AuthViewModelImpl(get()) }
}

fun AuthActivity.injectViewModel(): Lazy<AuthViewModel> =
    lazy { getViewModel<AuthViewModelImpl>(null) }