package com.gps.zazor.data.di

import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.prefs.AppPreferencesImpl
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val prefsModule = module {
    single<AppPreferences> { AppPreferencesImpl(androidApplication()) }
}