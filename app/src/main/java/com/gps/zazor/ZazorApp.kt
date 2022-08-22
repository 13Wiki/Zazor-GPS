package com.gps.zazor

import android.app.Application
import com.gps.zazor.data.storage.AppDatabaseCreator
import com.google.firebase.FirebaseApp
import com.gps.zazor.di.DependencyInjection

class ZazorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DependencyInjection.init(this)
        FirebaseApp.initializeApp(this)
        AppDatabaseCreator.createDatabase(this)
    }
}