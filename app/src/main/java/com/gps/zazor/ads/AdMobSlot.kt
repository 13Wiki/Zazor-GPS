package com.gps.zazor.ads

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ViewGroup
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.gps.zazor.BuildConfig

/**
 * AdMob behind [AdSlot].
 *
 * Every request carries `npa=1`, so ads are always non-personalised. That is not a setting someone
 * can flip later: the advertising id is stripped from the merged manifest, so the SDK has nothing
 * to personalise with even if the flag were removed. Non-personalised inventory pays less. It is
 * the price of an app whose whole claim is that it does not follow anyone, and paying it in
 * revenue is cheaper than paying it in credibility.
 *
 * With no unit id configured the slot reports itself unavailable and the app simply shows no ads.
 */
class AdMobSlot(context: Context) : AdSlot {

    private val appContext = context.applicationContext

    override val isAvailable: Boolean = BuildConfig.ADMOB_BANNER_UNIT_ID.isNotBlank()

    init {
        if (isAvailable) {
            MobileAds.initialize(appContext)
            // The store rating is 3+, so the inventory is capped at general audiences. Child
            // directed treatment is left unspecified, which is the default: the app is not aimed
            // at children, and asserting either way would be a claim we cannot stand behind.
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .build()
            )
        }
    }

    override fun show(container: ViewGroup): Boolean {
        if (!isAvailable) return false
        // Idempotent by design: a banner already in place means the request was already paid for.
        // Reloading on every resume is how an AdMob account gets flagged for invalid traffic, so
        // an existing AdView is left to its own refresh cycle rather than rebuilt.
        if (bannerOf(container) != null) return true

        val view = AdView(container.context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            setAdSize(adaptiveSize(container))
        }
        container.addView(view)
        view.loadAd(nonPersonalisedRequest())
        return true
    }

    override fun pause(container: ViewGroup) {
        bannerOf(container)?.pause()
    }

    override fun resume(container: ViewGroup) {
        bannerOf(container)?.resume()
    }

    override fun destroy(container: ViewGroup) {
        for (index in container.childCount - 1 downTo 0) {
            (container.getChildAt(index) as? AdView)?.destroy()
        }
        container.removeAllViews()
    }

    private fun bannerOf(container: ViewGroup): AdView? {
        for (index in 0 until container.childCount) {
            (container.getChildAt(index) as? AdView)?.let { return it }
        }
        return null
    }

    private fun nonPersonalisedRequest(): AdRequest =
        AdRequest.Builder()
            .addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                Bundle().apply { putString("npa", "1") }
            )
            .build()

    /**
     * An inline adaptive banner: the height follows the width, so the card is never a letterbox
     * strip on a wide phone or a squashed one on a narrow screen.
     */
    private fun adaptiveSize(container: ViewGroup): AdSize {
        val metrics: DisplayMetrics = container.resources.displayMetrics
        val widthPx = container.width.takeIf { it > 0 } ?: metrics.widthPixels
        val widthDp = (widthPx / metrics.density).toInt().coerceAtLeast(320)
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(container.context, widthDp)
    }
}
