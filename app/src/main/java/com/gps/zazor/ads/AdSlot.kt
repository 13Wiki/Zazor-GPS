package com.gps.zazor.ads

import android.view.ViewGroup

/**
 * Where an ad may appear.
 *
 * There is exactly one slot in the whole app - under the photo feed in the gallery - and this
 * interface is what enforces it. Nothing on the capture screen, in the editor or while sharing can
 * show an ad, because there is no way to ask for one there: a banner in front of someone mid-task
 * buys an uninstall, not an impression.
 *
 * The default implementation shows nothing. A build without an ad network configured is a working
 * build with no ads, not a crash.
 */
interface AdSlot {

    /** True when this build can actually show something. */
    val isAvailable: Boolean

    /**
     * Fills [container] with an ad, or leaves it empty and returns false.
     * Never called when the user has paid to remove ads.
     *
     * Idempotent: calling it while the container already holds this slot's ad is a no-op that
     * still returns true, so a screen may call it on every resume without paying for a fresh
     * request each time. A new request happens only after [destroy].
     */
    fun show(container: ViewGroup): Boolean

    /** Pauses the ad while the screen is not visible; safe to call when nothing is shown. */
    fun pause(container: ViewGroup)

    /** Resumes a paused ad; safe to call when nothing is shown. */
    fun resume(container: ViewGroup)

    fun destroy(container: ViewGroup)
}

/** Used until an ad network is configured; keeps every caller working. */
class NoAdSlot : AdSlot {

    override val isAvailable = false

    override fun show(container: ViewGroup) = false

    override fun pause(container: ViewGroup) = Unit

    override fun resume(container: ViewGroup) = Unit

    override fun destroy(container: ViewGroup) = Unit
}
