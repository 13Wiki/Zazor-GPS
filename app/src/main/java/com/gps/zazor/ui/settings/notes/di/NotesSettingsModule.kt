package com.gps.zazor.ui.settings.notes.di

import com.gps.zazor.ui.settings.notes.NotesSettingsFragment
import com.gps.zazor.ui.settings.notes.NotesSettingsViewModel
import com.gps.zazor.ui.settings.notes.NotesSettingsViewModelImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.dsl.module

val notesSettingsModule = module {
    viewModel { NotesSettingsViewModelImpl(get()) }
}

fun NotesSettingsFragment.injectViewModel(): Lazy<NotesSettingsViewModel> =
    lazy { getViewModel<NotesSettingsViewModelImpl>(null) }