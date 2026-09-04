package com.gps.zazor.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether this install has paid to remove the ads.
 *
 * Behind an interface so the rest of the app never talks to the billing library directly: the
 * gallery only asks "is this Pro", and a build with no Play services or no configured products
 * still runs, simply always answering no.
 */
interface ProStatus {

    val isPro: StateFlow<Boolean>

    /** Price as the store formats it for this user's country, or null until it is known. */
    val priceLabel: StateFlow<String?>

    /** Re-reads the purchase state; safe to call on every resume. */
    fun refresh()
}
