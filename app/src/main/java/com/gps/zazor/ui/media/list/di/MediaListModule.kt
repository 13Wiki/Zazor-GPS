package com.gps.zazor.ui.media.list.di

import com.gps.zazor.ui.media.list.MediaListFragment
import com.gps.zazor.ui.media.list.MediaListViewModel
import com.gps.zazor.ui.media.list.MediaListViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val mediaListModule = module {
    viewModel { MediaListViewModelImpl(get()) }
}

fun MediaListFragment.injectViewModel(): Lazy<MediaListViewModel> =
    lazy { getViewModel<MediaListViewModelImpl>(null) }