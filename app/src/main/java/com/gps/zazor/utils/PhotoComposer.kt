package com.gps.zazor.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible

/**
 * Draws the stamp and the marks onto the photo itself, at the photo's own resolution.
 *
 * The screen only ever shows a shrunken copy of the frame, letterboxed inside its container. The
 * saved picture used to be a screenshot of that container, so a twelve-megapixel capture reached
 * the gallery at screen size with the black bars baked into it - and this is an app whose whole
 * point is a photograph someone later has to prove something with. Here the marks are drawn onto
 * the captured bitmap instead, scaled from the rectangle the photo occupies on screen: a mark stays
 * on the same part of the picture it was put on, and nothing outside the frame is saved.
 */
object PhotoComposer {

    /**
     * The longest side a composed photo may have.
     *
     * A 48-megapixel frame is 8000 x 6000, and a mutable ARGB copy of it is 192 MB - more than a
     * mid-range phone will hand out. Twelve megapixels is far past what any report needs.
     */
    private const val MAX_SIDE = 4096

    /**
     * @param imageView the view showing the photo; it holds the frame and the mapping to it.
     * @param overlays views drawn over the photo, laid out inside [imageView]'s parent.
     * @return the composed photo, or null when the view is not showing a bitmap.
     */
    fun compose(imageView: ImageView, overlays: List<View>): Bitmap? {
        val source = (imageView.drawable as? BitmapDrawable)?.bitmap ?: return null
        val visible = overlays.filter { it.isVisible }
        val photoRect = photoRect(imageView)
        if (photoRect == null || visible.isEmpty()) return source.downscaledIfHuge()

        val scaled = source.downscaledIfHuge()
        val target = if (scaled !== source && scaled.isMutable) {
            scaled
        } else {
            scaled.copy(Bitmap.Config.ARGB_8888, true)
        } ?: return source

        val canvas = Canvas(target)
        // Points map as (point - rect.topLeft) * scale: the canvas applies the last transform first.
        val scale = target.width / photoRect.width()
        canvas.scale(scale, scale)
        canvas.translate(-photoRect.left, -photoRect.top)
        visible.forEach { overlay ->
            // draw() renders a view at the canvas origin, so its place in the parent is added here.
            canvas.save()
            canvas.translate(overlay.left.toFloat(), overlay.top.toFloat())
            overlay.draw(canvas)
            canvas.restore()
        }
        return target
    }

    /**
     * Lays [overlays] over exactly the photo, instead of over the whole screen.
     *
     * Marks belong to the picture rather than to the black around it: stretched across the
     * container, the stamp sat over a letterbox bar, where it looked right on screen and came back
     * cut off in the saved file.
     */
    fun fitOverlays(imageView: ImageView, overlays: List<View>) {
        val rect = photoRect(imageView) ?: return
        overlays.forEach { overlay ->
            val params = overlay.layoutParams as? ConstraintLayout.LayoutParams ?: return@forEach
            params.width = rect.width().toInt()
            params.height = rect.height().toInt()
            // A margin moves nothing until the view is anchored to something: without these two the
            // overlay kept the top-left corner and only its size changed.
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.leftMargin = rect.left.toInt()
            params.topMargin = rect.top.toInt()
            overlay.layoutParams = params
        }
    }

    /** Where the drawable actually lands inside the parent, with letterboxing taken out. */
    fun photoRect(imageView: ImageView): RectF? {
        val bounds = imageView.drawable ?: return null
        val rect = RectF(
            0F, 0F, bounds.intrinsicWidth.toFloat(), bounds.intrinsicHeight.toFloat()
        )
        imageView.imageMatrix.mapRect(rect)
        rect.offset(
            (imageView.left + imageView.paddingLeft).toFloat(),
            (imageView.top + imageView.paddingTop).toFloat()
        )
        return rect.takeIf { it.width() >= 1F && it.height() >= 1F }
    }

    private fun Bitmap.downscaledIfHuge(): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= MAX_SIDE) return this
        val factor = MAX_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            this, (width * factor).toInt(), (height * factor).toInt(), true
        )
    }
}
