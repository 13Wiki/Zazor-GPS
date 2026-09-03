package com.gps.zazor

import android.app.Application
import com.gps.zazor.di.DependencyInjection
import net.danlew.android.joda.JodaTimeAndroid

class ZazorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Loads the Joda time zone database from resources; without it every DateTime call on a
        // device with an unusual locale can throw.
        JodaTimeAndroid.init(this)
        DependencyInjection.init(this)
    }
}
