package com.gps.zazor.ui.settings

interface SettingsCallback {

    fun openPinSetup()

    fun openClearCodeSetup()

    fun openNotesSettings()

    /** Decimal degrees or degrees-minutes-seconds, for what is written on the picture. */
    fun openCoordinateFormat()

    /** Writing to the author: what is broken, and what the person wishes the app did. */
    fun openFeedback()

    /** Launcher icon, label and a reminder of what the wipe code does. */
    fun openAppearance()

    /** What the app collects, and the switch to refuse it. */
    fun openPrivacy()

    /** Starts the purchase that removes the ads. */
    fun openPro()
}
