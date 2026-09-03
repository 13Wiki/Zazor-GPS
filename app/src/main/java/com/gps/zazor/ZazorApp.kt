package com.gps.zazor

import android.app.Application
import com.gps.zazor.di.DependencyInjection

class ZazorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DependencyInjection.init(this)
    }
}
