package com.gps.zazor.ui.outings.di

import com.gps.zazor.ui.outings.OutingMapFragment
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

/**
 * The map of one outing reads the same outings as the list does, from its own instance: the day it
 * is looking at is its own business, and going back must not move the list's selection.
 */
fun OutingMapFragment.injectMapViewModel(): Lazy<OutingsViewModel> =
    lazy { getViewModel<OutingsViewModelImpl>() }
