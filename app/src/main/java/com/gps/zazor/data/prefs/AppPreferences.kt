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

    override fun clear() {
        preferences.edit().clear().commit()
    }
}