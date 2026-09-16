package com.gps.zazor.ui.settings.list.di

import com.gps.zazor.ui.settings.list.SettingsListFragment
import com.gps.zazor.ui.settings.list.SettingsListViewModel
import com.gps.zazor.ui.settings.list.SettingsListViewModelImpl
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val settingsListModule = module {
    // The rows carry their own values, and values are words: the list needs resources to read.
    viewModel { SettingsListViewModelImpl(androidApplication(), get(), get()) }
}

fun SettingsListFragment.injectViewModel(): Lazy<SettingsListViewModel> =
    lazy { getViewModel<SettingsListViewModelImpl>() }
