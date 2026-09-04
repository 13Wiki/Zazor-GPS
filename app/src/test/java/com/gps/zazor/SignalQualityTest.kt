package com.gps.zazor

import com.gps.zazor.utils.location.SignalQuality
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shutter decides whether to warn from this object, so its edges matter: no fix at all is not
 * the same as a rough fix, and a fix exactly on the threshold is still good.
 */
class SignalQualityTest {

    @Test
    fun `no fix yet is never acceptable`() {
        val quality = SignalQuality.waiting(thresholdMeters = 10, warnBeforeCapture = true)

        assertFalse(quality.hasFix)
        assertFalse(quality.isAcceptable)
        assertTrue(quality.shouldWarn)
    }

    @Test
    fun `a fix inside the threshold passes`() {
        val quality = SignalQuality(4F, 10, warnBeforeCapture = true)

        assertTrue(quality.isAcceptable)
        assertFalse(quality.shouldWarn)
    }

    @Test
    fun `a fix exactly on the threshold still passes`() {
        assertTrue(SignalQuality(10F, 10, warnBeforeCapture = true).isAcceptable)
    }

    @Test
    fun `a fix past the threshold warns`() {
        val quality = SignalQuality(10.1F, 10, warnBeforeCapture = true)

        assertFalse(quality.isAcceptable)
        assertTrue(quality.shouldWarn)
    }

    @Test
    fun `with the warning switched off a rough fix is stamped silently`() {
        val quality = SignalQuality(50F, 10, warnBeforeCapture = false)

        assertFalse(quality.isAcceptable)
        assertFalse(quality.shouldWarn)
    }
}
