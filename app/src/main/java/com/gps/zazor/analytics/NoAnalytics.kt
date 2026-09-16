package com.gps.zazor.analytics

/**
 * What the app counts when no Firebase project is configured: nothing.
 *
 * This is the implementation a build without `google-services.json` gets. It is not a stub waiting
 * to be replaced - it is the honest answer for a release that ships without telemetry, and it keeps
 * every caller free of "if analytics exists" checks.
 */
class NoAnalytics : Analytics {

    override val isAvailable: Boolean = false

    override fun track(event: Analytics.Event, count: Int?) = Unit

    override fun setEnabled(enabled: Boolean) = Unit
}
