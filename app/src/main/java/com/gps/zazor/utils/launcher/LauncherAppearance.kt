package com.gps.zazor.utils.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gps.zazor.R

/**
 * Swaps the icon and the name shown on the home screen.
 *
 * Done with activity aliases: exactly one is enabled at a time and the rest are disabled, which is
 * the only way Android lets an app change how it is listed.
 *
 * The app is never hidden from the launcher. Google reads full concealment as deceptive behaviour
 * and pulls the listing — and losing the listing takes everything else with it. Changing the icon
 * and the label is ordinary and allowed; disappearing is not.
 */
class LauncherAppearance(private val context: Context) {

    enum class Appearance(
        val aliasSuffix: String,
        @DrawableRes val previewIcon: Int,
        @StringRes val title: Int
    ) {
        DEFAULT(".ui.auth.AuthActivity", R.mipmap.ic_launcher, R.string.appearance_default),
        NOTES(".Alias.Notes", R.drawable.ic_appearance_notes, R.string.appearance_notes),
        CLOCK(".Alias.Clock", R.drawable.ic_appearance_clock, R.string.appearance_clock),
        COMPASS(".Alias.Compass", R.drawable.ic_appearance_compass, R.string.appearance_compass)
    }

    private val packageManager: PackageManager get() = context.packageManager

    fun current(): Appearance =
        Appearance.entries.firstOrNull { isEnabled(it) } ?: Appearance.DEFAULT

    /**
     * Enables one appearance and disables the others.
     *
     * The launcher briefly drops and re-adds the icon while this happens; that is unavoidable and
     * is why it is not offered as a casual toggle.
     */
    fun apply(appearance: Appearance) {
        if (current() == appearance) return
        // Enable the target first: disabling every alias at once would remove the app from the
        // launcher entirely until the next one is enabled.
        setState(appearance, enabled = true)
        Appearance.entries.filter { it != appearance }.forEach { setState(it, enabled = false) }
    }

    private fun isEnabled(appearance: Appearance): Boolean =
        try {
            packageManager.getComponentEnabledSetting(componentOf(appearance)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (e: IllegalArgumentException) {
            false
        }

    private fun setState(appearance: Appearance, enabled: Boolean) {
        try {
            packageManager.setComponentEnabledSetting(
                componentOf(appearance),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            // Some launchers refuse the change; the app keeps working with the icon it has.
        }
    }

    private fun componentOf(appearance: Appearance) =
        ComponentName(context, context.packageName + appearance.aliasSuffix)
}
