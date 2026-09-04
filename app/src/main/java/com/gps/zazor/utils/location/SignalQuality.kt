package com.gps.zazor.utils.location

/**
 * How trustworthy the current fix is, as the capture screen needs to show it.
 *
 * Kept out of the screen's UiState on purpose: it updates every couple of seconds, and folding it
 * into a conflated StateFlow alongside capture and preview states would let a position update
 * swallow them.
 *
 * @param accuracyMeters radius reported by the provider, or `null` while there is no fix at all.
 * @param thresholdMeters the value above which a fix counts as too rough.
 * @param warnBeforeCapture whether the user asked to be warned; when off, a rough fix is stamped
 *        silently.
 */
data class SignalQuality(
    val accuracyMeters: Float?,
    val thresholdMeters: Int,
    val warnBeforeCapture: Boolean
) {

    /** True once a fix exists and it is within the threshold. */
    val isAcceptable: Boolean
        get() = accuracyMeters != null && accuracyMeters <= thresholdMeters

    /** True when the shutter should ask for confirmation first. */
    val shouldWarn: Boolean
        get() = warnBeforeCapture && !isAcceptable

    val hasFix: Boolean
        get() = accuracyMeters != null

    companion object {

        /**
         * How old a fix may be and still count. The fused provider hands out its cached last
         * position first, which can be hours old: without this the indicator would report a
         * confident 4 m from a stale fix in exactly the situation the warning exists for.
         */
        const val MAX_FIX_AGE_MS = 90_000L

        fun waiting(thresholdMeters: Int, warnBeforeCapture: Boolean) =
            SignalQuality(null, thresholdMeters, warnBeforeCapture)
    }
}
