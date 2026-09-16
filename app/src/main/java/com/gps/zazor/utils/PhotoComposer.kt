package com.gps.zazor.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlin.math.sqrt

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

    private const val BYTES_PER_PIXEL = 4

    /**
     * How much of the heap a composed photo may take.
     *
     * It is copied, drawn onto and then encoded, so a quarter leaves room for the encoder and for
     * whatever else the screen is holding.
     */
    private const val HEAP_SHARE = 4

    /** Under this the picture stops being a record of anything, and giving up is the better answer. */
    private const val MIN_LONG_SIDE = 1600

    private const val RETRIES = 3

    /**
     * @param imageView the view showing the photo; it holds the frame and the mapping to it.
     * @param overlays views drawn over the photo, laid out inside [imageView]'s parent.
     * @return the composed photo, or null when there is nothing to compose onto or no room for it.
     */
    fun compose(imageView: ImageView, overlays: List<View>): Bitmap? {
        val source = (imageView.drawable as? BitmapDrawable)?.bitmap ?: return null
        val target = workingCopy(source) ?: return null
        val visible = overlays.filter { it.isVisible }
        val photoRect = photoRect(imageView)
        if (photoRect == null || visible.isEmpty()) return target

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

    /**
     * A mutable copy of the frame, as large as this particular phone can hold.
     *
     * The ceiling is a share of the app's own heap rather than a number someone picked: the same
     * build runs on a phone that gets 96 MB and on one that gets 512, and a 200-megapixel flagship
     * frame would ask for 800 MB as an editable copy. A phone that can keep the whole frame keeps
     * it; one that cannot gets the largest picture it can actually hold. If the allocation fails
     * anyway - the heap is shared with everything else on screen - it steps down and tries again,
     * and gives up only below the point where the photo would stop being worth keeping. Giving up
     * is not losing the shot: the caller then saves what is on screen, stamp and all.
     */
    private fun workingCopy(source: Bitmap): Bitmap? {
        val pixels = source.width.toLong() * source.height
        val budget = Runtime.getRuntime().maxMemory() / HEAP_SHARE / BYTES_PER_PIXEL
        var factor = if (pixels > budget) sqrt(budget.toDouble() / pixels).toFloat() else 1F
        repeat(RETRIES) {
            val width = (source.width * factor).toInt().coerceAtLeast(1)
            val height = (source.height * factor).toInt().coerceAtLeast(1)
            try {
                val copy = if (width == source.width && height == source.height) {
                    source.copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    Bitmap.createScaledBitmap(source, width, height, true)
                }
                if (copy != null) return if (copy.isMutable) copy else copy.copy(Bitmap.Config.ARGB_8888, true)
            } catch (e: OutOfMemoryError) {
                // Nothing to release here - the failed allocation never happened.
            }
            factor *= 0.7F
            if (maxOf(source.width, source.height) * factor < MIN_LONG_SIDE) return null
        }
        return null
    }
}
