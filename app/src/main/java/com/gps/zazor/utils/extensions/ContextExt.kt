package com.gps.zazor.utils.extensions

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

fun Context.hasBiometric(): Boolean =
    BiometricManager.from(this)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS

/**
 * Walks up the [ContextWrapper] chain to the hosting activity.
 *
 * A view's context is usually a `ContextThemeWrapper`, so casting it straight to an activity or a
 * `LifecycleOwner` fails - which is why the edit sheet's state collectors never started.
 */
fun Context.findActivity(): ComponentActivity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}
