package com.gps.zazor.ui.settings.appearance.di

import com.gps.zazor.ui.settings.appearance.AppearanceFragment
import com.gps.zazor.ui.settings.appearance.AppearanceViewModel
import com.gps.zazor.ui.settings.appearance.AppearanceViewModelImpl
import com.gps.zazor.utils.launcher.LauncherAppearance
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val appearanceModule = module {
    single { LauncherAppearance(androidApplication()) }
    viewModel { AppearanceViewModelImpl(get(), get()) }
}

fun AppearanceFragment.injectViewModel(): Lazy<AppearanceViewModel> =
    lazy { getViewModel<AppearanceViewModelImpl>() }
