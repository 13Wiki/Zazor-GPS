package com.gps.zazor.ui.privacy.di

import com.gps.zazor.ui.privacy.PrivacyFragment
import com.gps.zazor.ui.privacy.PrivacyViewModel
import com.gps.zazor.ui.privacy.PrivacyViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val privacyModule = module {
    viewModel { PrivacyViewModelImpl(get(), get()) }
}

fun PrivacyFragment.injectViewModel(): Lazy<PrivacyViewModel> =
    lazy { getViewModel<PrivacyViewModelImpl>() }
