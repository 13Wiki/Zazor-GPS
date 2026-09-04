package com.gps.zazor.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.gps.zazor.data.prefs.AppPreferences

/**
 * Firebase behind [Analytics].
 *
 * Collection is disabled in the manifest by default and only switched on once the person has said
 * yes, so a first launch sends nothing while the choice is still on screen. Advertising ID
 * collection stays off permanently: nothing here should be able to follow anyone between apps.
 */
class FirebaseAnalyticsClient(
    context: Context,
    private val prefs: AppPreferences
) : Analytics {

    private val firebase = FirebaseAnalytics.getInstance(context.applicationContext)

    init {
        applyConsent(prefs.isAnalyticsEnabled() && prefs.isPrivacyAccepted())
    }

    override fun track(event: Analytics.Event, count: Int?) {
        if (!prefs.isAnalyticsEnabled()) return
        val params = Bundle().apply { count?.let { putInt("count", it) } }
        firebase.logEvent(event.key, params)
    }

    override fun setEnabled(enabled: Boolean) {
        prefs.setAnalyticsEnabled(enabled)
        applyConsent(enabled)
    }

    private fun applyConsent(enabled: Boolean) {
        firebase.setAnalyticsCollectionEnabled(enabled)
    }
}
