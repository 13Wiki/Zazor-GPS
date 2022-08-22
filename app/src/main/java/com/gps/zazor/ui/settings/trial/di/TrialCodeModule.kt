package com.gps.zazor.ui.settings.trial.di

import com.gps.zazor.ui.settings.trial.TrialCodeFragment
import com.gps.zazor.ui.settings.trial.TrialCodeViewModel
import com.gps.zazor.ui.settings.trial.TrialCodeViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val trialCodeModule = module {
    viewModel { TrialCodeViewModelImpl(get()) }
}

fun TrialCodeFragment.injectViewModel(): Lazy<TrialCodeViewModel> =
    lazy { getViewModel<TrialCodeViewModelImpl>(null) }