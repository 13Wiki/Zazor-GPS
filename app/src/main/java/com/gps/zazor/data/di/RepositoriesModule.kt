package com.gps.zazor.data.di

import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.data.repositories.PhotoRepositoryImpl
import com.gps.zazor.data.storage.AppDatabase
import com.gps.zazor.utils.PhotoStorage
import com.gps.zazor.utils.location.AddressResolver
import com.gps.zazor.utils.location.LocationProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val repositoriesModule = module {
    single { AppDatabase.create(androidApplication()) }
    single { get<AppDatabase>().photosDao() }
    single<PhotoRepository> { PhotoRepositoryImpl(get(), get()) }
    single { PhotoStorage(androidApplication()) }
    single { LocationProvider(androidApplication()) }
    single { AddressResolver(androidApplication()) }
}
