package com.gps.zazor.ui.share.di

import com.gps.zazor.ui.share.ShareFragment
import com.gps.zazor.ui.share.ShareViewModel
import com.gps.zazor.ui.share.ShareViewModelImpl
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val shareModule = module {
    viewModel { ShareViewModelImpl(androidContext(), get(), get()) }
}

fun ShareFragment.injectViewModel(): Lazy<ShareViewModel> =
    lazy { getViewModel<ShareViewModelImpl>() }
