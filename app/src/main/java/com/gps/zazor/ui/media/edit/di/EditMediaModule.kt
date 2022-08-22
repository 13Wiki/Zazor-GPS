package com.gps.zazor.ui.media.edit.di

import com.gps.zazor.ui.media.edit.EditMediaFragment
import com.gps.zazor.ui.media.edit.EditMediaViewModel
import com.gps.zazor.ui.media.edit.EditMediaViewModelImpl
import com.gps.zazor.ui.photo.di.ADD_NOTE_FLOW
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val editMediaModule = module {
    viewModel {
        EditMediaViewModelImpl(
            androidContext(),
            get(named(ADD_NOTE_FLOW)),
            get(),
            get()
        )
    }
}

fun EditMediaFragment.injectViewModel(): Lazy<EditMediaViewModel> =
    lazy { getViewModel<EditMediaViewModelImpl>(null) }