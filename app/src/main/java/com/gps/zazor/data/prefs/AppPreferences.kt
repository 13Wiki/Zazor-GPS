package com.gps.zazor.data.prefs

import android.annotation.SuppressLint
import android.content.Context

interface AppPreferences {

    fun putPin(pin: String?)

    fun getPin(): String?

    fun putClearCode(code: String?)

    fun getClearCode(): String?

    fun putDisplayCoordinates(isDisplay: Boolean)

    fun isDisplayCoordinates(): Boolean

    fun putDisplayTime(isDisplay: Boolean)

    fun isDisplayTime(): Boolean

    fun putDisplayDate(isDisplay: Boolean)

    fun isDisplayAccuracy(): Boolean

    fun putDisplayAccuracy(isDisplay: Boolean)

    fun isDisplayDate(): Boolean

    fun setTrial(trial: Boolean)

    fun isTrial(): Boolean

    fun putDrawColor(color: Int)

    fun getDrawColor(): Int?

    fun putTextColor(color: Int)

    fun getTextColor(): Int?

    fun putFont(fontId: Int)

    fun getFont(): Int?

    /** Warn before shooting while the fix is worse than [getAccuracyThresholdMeters]. */
    fun isWaitForAccurateFix(): Boolean

    fun putWaitForAccurateFix(wait: Boolean)

    /** Accuracy in metres above which a fix counts as too rough to stamp. */
    fun getAccuracyThresholdMeters(): Int

    fun putAccuracyThresholdMeters(meters: Int)

    /** Mirror of the Play entitlement, so ads stay off while offline. The store stays authority. */
    fun isPro(): Boolean

    fun setPro(pro: Boolean)

    /** False until the person has seen what the app does and does not collect. */
    fun isPrivacyAccepted(): Boolean

    fun setPrivacyAccepted(accepted: Boolean)

    /** Whether anonymous counters may be sent at all. */
    fun isAnalyticsEnabled(): Boolean

    fun setAnalyticsEnabled(enabled: Boolean)

    fun clear()
}

@SuppressLint("ApplySharedPref")
class AppPreferencesImpl(context: Context) : AppPreferences {

    companion object {

        private const val PREFS = "PREFS"
        private const val PIN_KEY = "pinCode"
        private const val CLEAR_CODE_KEY = "clearCode"
        private const val DISPLAY_COORDINATES_KEY = "displayCoordinates"
        private const val DISPLAY_DATE_KEY = "displayDate"
        private const val DISPLAY_ACCURACY = "displayAccuracy"
        private const val DISPLAY_TIME_KEY = "displayTime"
        private const val TRIAL_KEY = "trialKey"
        private const val DRAW_COLOR_KEY = "drawColor"
        private const val TEXT_COLOR_KEY = "textColor"
        private const val FONT_KEY = "font"
        private const val WAIT_FIX_KEY = "waitForAccurateFix"
        private const val ACCURACY_THRESHOLD_KEY = "accuracyThresholdMeters"
        private const val PRO_KEY = "pro"
        private const val PRIVACY_KEY = "privacyAccepted"
        private const val ANALYTICS_KEY = "analyticsEnabled"

        /** A phone in the open reaches 3-5 m; beyond 10 m the point is no longer worth stamping. */
        const val DEFAULT_ACCURACY_THRESHOLD_M = 10

        private const val NO_VALUE = -1
    }

    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun putPin(pin: String?) {
        preferences.edit().putString(PIN_KEY, pin).commit()
    }

    override fun getPin(): String? {
        return preferences.getString(PIN_KEY, null)
    }

    override fun putClearCode(code: String?) {
        preferences.edit().putString(CLEAR_CODE_KEY, code).commit()
    }

    override fun getClearCode(): String? {
        return preferences.getString(CLEAR_CODE_KEY, null)
    }

    override fun putDisplayCoordinates(isDisplay: Boolean) {
        preferences.edit().putBoolean(DISPLAY_COORDINATES_KEY, isDisplay).commit()
    }

    override fun isDisplayCoordinates(): Boolean {
        return preferences.getBoolean(DISPLAY_COORDINATES_KEY, true)
    }

    override fun putDisplayDate(isDisplay: Boolean) {
        preferences.edit().putBoolean(DISPLAY_DATE_KEY, isDisplay).commit()
    }

    override fun isDisplayDate(): Boolean {
        return preferences.getBoolean(DISPLAY_DATE_KEY, true)
    }

    override fun putDisplayAccuracy(isDisplay: Boolean) {
        preferences.edit().putBoolean(DISPLAY_ACCURACY, isDisplay).commit()
    }

    override fun isDisplayAccuracy(): Boolean {
        return preferences.getBoolean(DISPLAY_ACCURACY, true)
    }

    override fun putDisplayTime(isDisplay: Boolean) {
        preferences.edit().putBoolean(DISPLAY_TIME_KEY, isDisplay).commit()
    }

    override fun isDisplayTime(): Boolean {
        return preferences.getBoolean(DISPLAY_TIME_KEY, true)
    }

    override fun setTrial(trial: Boolean) {
        preferences.edit().putBoolean(TRIAL_KEY, trial).commit()
    }

    override fun isTrial(): Boolean {
        return preferences.getBoolean(TRIAL_KEY, true)
    }

    override fun putDrawColor(color: Int) {
        preferences.edit().putInt(DRAW_COLOR_KEY, color).commit()
    }

    override fun getDrawColor(): Int? {
        return preferences.getInt(DRAW_COLOR_KEY, NO_VALUE).takeUnless { it == NO_VALUE }
    }

    override fun putTextColor(color: Int) {
        preferences.edit().putInt(TEXT_COLOR_KEY, color).commit()
    }

    override fun getTextColor(): Int? {
        return preferences.getInt(TEXT_COLOR_KEY, NO_VALUE).takeUnless { it == NO_VALUE }
    }

    override fun putFont(fontId: Int) {
        preferences.edit().putInt(FONT_KEY, fontId).commit()
    }

    override fun getFont(): Int? {
        return preferences.getInt(FONT_KEY, NO_VALUE).takeUnless { it == NO_VALUE }
    }

    override fun isWaitForAccurateFix(): Boolean =
        preferences.getBoolean(WAIT_FIX_KEY, true)

    override fun putWaitForAccurateFix(wait: Boolean) {
        preferences.edit().putBoolean(WAIT_FIX_KEY, wait).commit()
    }

    override fun getAccuracyThresholdMeters(): Int =
        preferences.getInt(ACCURACY_THRESHOLD_KEY, DEFAULT_ACCURACY_THRESHOLD_M)

    override fun putAccuracyThresholdMeters(meters: Int) {
        preferences.edit().putInt(ACCURACY_THRESHOLD_KEY, meters).commit()
    }

    override fun isPro(): Boolean = preferences.getBoolean(PRO_KEY, false)

    override fun setPro(pro: Boolean) {
        preferences.edit().putBoolean(PRO_KEY, pro).commit()
    }

    override fun isPrivacyAccepted(): Boolean = preferences.getBoolean(PRIVACY_KEY, false)

    override fun setPrivacyAccepted(accepted: Boolean) {
        preferences.edit().putBoolean(PRIVACY_KEY, accepted).commit()
    }

    override fun isAnalyticsEnabled(): Boolean = preferences.getBoolean(ANALYTICS_KEY, true)

    override fun setAnalyticsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(ANALYTICS_KEY, enabled).commit()
    }

    /**
     * Wipes everything the app remembers - including the entitlement mirror, which Play restores
     * on the next query. The wipe code must not leave a trace of who used this phone.
     */
    override fun clear() {
        preferences.edit().clear().commit()
    }
}