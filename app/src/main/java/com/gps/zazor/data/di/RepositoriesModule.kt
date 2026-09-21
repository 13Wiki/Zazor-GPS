package com.gps.zazor.data.di

import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.data.repositories.PhotoRepositoryImpl
import com.gps.zazor.data.storage.AppDatabase
import com.gps.zazor.utils.PhotoStorage
import com.gps.zazor.ads.AdSlot
import com.gps.zazor.analytics.Analytics
import com.gps.zazor.analytics.NoAnalytics
import com.gps.zazor.ads.AdMobSlot
import com.gps.zazor.ads.NoAdSlot
import com.gps.zazor.billing.PlayProStatus
import com.gps.zazor.billing.ProStatus
import com.gps.zazor.utils.export.BundleWriter
import com.gps.zazor.utils.export.TrackFileWriter
import com.gps.zazor.utils.location.AddressResolver
import com.gps.zazor.utils.export.PdfReportWriter
import com.gps.zazor.utils.location.CompassProvider
import com.gps.zazor.utils.location.LocationProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val repositoriesModule = module {
    single { AppDatabase.create(androidApplication()) }
    single { get<AppDatabase>().photosDao() }
    single { PhotoStorage(androidApplication()) }
    single { LocationProvider(androidApplication()) }
    single { CompassProvider(androidApplication()) }
    single { PdfReportWriter(androidApplication()) }
    single { AddressResolver(androidApplication()) }
    single { TrackFileWriter(androidApplication()) }
    single { BundleWriter(androidApplication()) }
    single<PhotoRepository> { PhotoRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ProStatus> { PlayProStatus(androidApplication(), get()) }
    // No ad network configured yet: a build with no ads is a working build, not a crash.
    // NoAdSlot stays as the fallback: AdMobSlot reports itself unavailable without a unit id, but
    // keeping the empty implementation means a build can drop the ad network entirely by one line.
    single<AdSlot> { AdMobSlot(androidApplication()) }
    /*
     * Counting is a call into nothing, because this build carries no analytics library at all.
     *
     * The call sites stay - they say what the app would count and cost nothing - so restoring
     * telemetry is a matter of putting an implementation back here, not of finding the places.
     */
    single<Analytics> { NoAnalytics() }
}
