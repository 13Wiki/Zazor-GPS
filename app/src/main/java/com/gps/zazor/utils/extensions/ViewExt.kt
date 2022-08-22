package com.gps.zazor.utils.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.TextView

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showOrGone(show: Boolean) {
    if (show) show() else gone()
}

fun View.showOrHide(show: Boolean) {
    if (show) show() else invisible()
}

fun TextView.goneIfEmpty(text: String?) {
    text?.let {
        show()
        this.text = it
    } ?: gone()
}

fun View.disableClickTemporarily() {
    isClickable = false
    postDelayed({
        isClickable = true
    }, 500)
}

val View.screenHeight
    get() = context?.resources?.displayMetrics?.heightPixels ?: 0

val View.screenWidth
    get() = context?.resources?.displayMetrics?.widthPixels ?: 0

/**
 * Get Bitmap from [this] View.
 *
 * @param config [Bitmap.Config] bitmap configuration
 *
 * @return [Bitmap] bitmap with view screenshot
 */
fun View.getBitmap(config: Bitmap.Config = Bitmap.Config.ARGB_8888) =
    takeIf { hasSize() }?.run {
        val bmp = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(bmp)
        draw(canvas)
        return@run bmp
    }

fun View.hasSize() = width > 0 && height > 0