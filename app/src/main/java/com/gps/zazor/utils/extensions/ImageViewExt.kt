package com.gps.zazor.utils.extensions

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.gps.zazor.R
import java.io.File

val ImageView.bitmap
    get() = (drawable as? BitmapDrawable)?.bitmap

/*
 * Glide's builder methods return a *new* builder rather than mutating the receiver. The previous
 * versions called `placeholder`, `error` and `circleCrop` inside an `apply { }` and threw the
 * results away, so none of those options ever took effect.
 */

fun ImageView.loadImage(bitmap: Bitmap, @DrawableRes placeholder: Int? = null) {
    var request = Glide.with(context)
        .load(bitmap)
        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
    placeholder?.let { request = request.placeholder(it).error(it) }
    request.into(this)
}

/**
 * @param circle crops to a circle.
 * @param cornerRadiusDp crops to a rounded square instead; the design frames a thumbnail that
 *        way, and a circle cannot be made to look like one by any amount of background.
 */
fun ImageView.loadImage(
    uri: String?,
    circle: Boolean = true,
    @DrawableRes placeholder: Int? = null,
    cornerRadiusDp: Int? = null
) {
    show()
    var request = Glide.with(context)
        .load(uri?.let(::File))
        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
    placeholder?.let { request = request.placeholder(it).error(it) }
    when {
        cornerRadiusDp != null -> {
            val radius = (cornerRadiusDp * resources.displayMetrics.density).toInt()
            request = request.transform(CenterCrop(), RoundedCorners(radius))
        }
        circle -> request = request.circleCrop()
    }
    request.into(this)
}

fun ImageView.toggle(isSelected: Boolean) {
    val color =
        ContextCompat.getColor(context, if (isSelected) R.color.colorAccent else R.color.gray)
    // mutate() before tinting: resource drawables share a ConstantState, so tinting one view's
    // drawable silently recolours every other view showing the same icon.
    drawable?.mutate()?.setTint(color)
}
