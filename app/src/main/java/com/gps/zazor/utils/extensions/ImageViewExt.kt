package com.gps.zazor.utils.extensions

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.gps.zazor.R
import java.io.File

val ImageView.bitmap
    get() = (drawable as? BitmapDrawable)?.bitmap

fun ImageView.loadImage(bitmap: Bitmap, placeholder: Int? = null) {
    Glide.with(context)
        .load(bitmap)
        .apply {
            placeholder?.let {
                placeholder(it)
                error(it)
            }
        }
        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
        .into(this)
}

fun ImageView.loadImage(uri: String?, circle: Boolean = true, placeholder: Int? = null) {
    show()
    Glide.with(context)
        .load(uri?.let(::File))
        .apply {
            placeholder?.let {
                placeholder(it)
                error(it)
            }
            if (circle) circleCrop()
        }
        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
        .into(this)
}

fun ImageView.toggle(isSelected: Boolean) {
    val color =
        ContextCompat.getColor(context, if (isSelected) R.color.colorAccent else R.color.gray)
    drawable?.setTint(color)
}

/*
fun ImageView.overlayNotes(notes: String) {
    bitmap?.overlayNotes(notes)?.let(::loadImage)
}*/
