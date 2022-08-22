package com.gps.zazor.data.di

import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.data.repositories.PhotoRepositoryImpl
import com.gps.zazor.data.storage.AppDatabaseCreator
import org.koin.dsl.module

val repositoriesModule = module {
    single { AppDatabaseCreator.database.photosDao() }
    single<PhotoRepository> { PhotoRepositoryImpl(get()) }
}