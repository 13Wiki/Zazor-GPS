package com.gps.zazor.ui.settings

interface SettingsCallback {

    fun openPinSetup()

    fun openClearCodeSetup()

    fun openNotesSettings()

    fun openTrialCode()

    /** Launcher icon, label and a reminder of what the wipe code does. */
    fun openAppearance()

    /** What the app collects, and the switch to refuse it. */
    fun openPrivacy()

    /** Starts the purchase that removes the ads. */
    fun openPro()
}
