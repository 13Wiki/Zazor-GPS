package com.gps.zazor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.gps.zazor.R
import com.gps.zazor.data.models.Outing
import com.gps.zazor.data.models.Photo
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Draws where the person walked and where each shot was taken.
 *
 * Deliberately not a map SDK. A tile map would need a Google Maps key (and a billing account
 * behind it), a network connection, and roughly 3 MB of library — for a screen whose whole job is
 * "which of these points is which, and how far apart are they". Drawn here, it costs nothing,
 * works with no signal in the middle of a field, and there is no key to leak or expire. Whoever
 * wants real streets under the track taps "open in maps" and gets their own map app.
 */
class RouteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {

        private const val PADDING_DP = 40F
        private const val POINT_RADIUS_DP = 13F
        private const val TRACK_WIDTH_DP = 3F
        private const val TOUCH_SLOP_DP = 24F
        private const val SCALE_BAR_MAX_FRACTION = 0.35F
        /** Metres of span assumed when every shot was taken from one spot. */
        private const val SINGLE_POINT_SPAN_M = 60.0
    }

    private val accent = ContextCompat.getColor(context, R.color.ds_accent)
    private val danger = ContextCompat.getColor(context, R.color.ds_danger)
    private val ink = ContextCompat.getColor(context, R.color.ds_text_primary)
    private val muted = ContextCompat.getColor(context, R.color.ds_text_muted)
    private val surface = ContextCompat.getColor(context, R.color.ds_background)

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = accent
        alpha = 46
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = accent
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        style = Paint.Style.STROKE
    }
    private val scaleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted }

    private val padding = PADDING_DP.dp()
    private val pointRadius = POINT_RADIUS_DP.dp()
    private val touchSlop = TOUCH_SLOP_DP.dp()

    private val projected = mutableListOf<PointF>()
    private val trackPath = Path()
    private val textBounds = Rect()

    /** Located photos only, oldest first - the order the track is walked in. */
    private var points: List<Photo> = emptyList()

    /** Metres represented by one pixel, recomputed on every layout. */
    private var metersPerPixel = 1.0

    var selectedIndex: Int = -1
        set(value) {
            field = value
            invalidate()
        }

    /** Invoked with the index into the located points, or -1 when the tap missed. */
    var onPointSelected: ((Int) -> Unit)? = null

    init {
        trackPaint.strokeWidth = TRACK_WIDTH_DP.dp()
        haloPaint.strokeWidth = TRACK_WIDTH_DP.dp() * 4
        ringPaint.strokeWidth = 3F.dp()
        labelPaint.textSize = 12F.sp()
        scalePaint.strokeWidth = 2F.dp()
        scaleTextPaint.textSize = 11F.sp()
        isClickable = true
        isFocusable = true
    }

    /** @param photos any photos; those without a fix are ignored. */
    fun setPhotos(photos: List<Photo>) {
        points = photos.filter { it.lat != null && it.lng != null }.sortedBy { it.date }
        selectedIndex = points.lastIndex
        project()
        invalidate()
    }

    /** The photo behind a drawn point, or null when the index is out of range. */
    fun photoAt(index: Int): Photo? = points.getOrNull(index)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        project()
    }

    /**
     * Equirectangular projection, fitted to the view.
     *
     * Longitude degrees shrink towards the poles, so they are scaled by cos(latitude) - without it
     * a north-south walk in Kyiv would come out visibly skewed. Over a few kilometres the error of
     * treating the result as flat is far below a pixel.
     */
    private fun project() {
        projected.clear()
        trackPath.reset()
        if (points.isEmpty() || width == 0 || height == 0) return

        val midLat = points.sumOf { it.lat!! } / points.size
        val lonScale = cos(Math.toRadians(midLat)).coerceAtLeast(0.01)

        val xs = points.map { it.lng!! * lonScale }
        val ys = points.map { -it.lat!! } // screen y grows downwards

        val spanX = (xs.max() - xs.min()).takeIf { it > 0 } ?: 0.0
        val spanY = (ys.max() - ys.min()).takeIf { it > 0 } ?: 0.0

        val usableW = (width - padding * 2).coerceAtLeast(1F)
        val usableH = (height - padding * 2).coerceAtLeast(1F)

        // A single point, or several from one spot, has no span: centre it at a fixed zoom.
        val degreesPerMeter = 1.0 / 111_320.0
        val fallbackSpan = SINGLE_POINT_SPAN_M * degreesPerMeter
        val effectiveSpanX = max(spanX, fallbackSpan)
        val effectiveSpanY = max(spanY, fallbackSpan)

        val scale = minOf(usableW / effectiveSpanX, usableH / effectiveSpanY)

        val centreX = (xs.max() + xs.min()) / 2
        val centreY = (ys.max() + ys.min()) / 2

        points.indices.forEach { i ->
            val x = (width / 2F + ((xs[i] - centreX) * scale)).toFloat()
            val y = (height / 2F + ((ys[i] - centreY) * scale)).toFloat()
            projected.add(PointF(x, y))
            if (i == 0) trackPath.moveTo(x, y) else trackPath.lineTo(x, y)
        }

        // One pixel in metres: latitude degrees are constant, so derive from the vertical scale.
        metersPerPixel = 1.0 / (scale * degreesPerMeter)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (projected.isEmpty()) return

        if (projected.size > 1) {
            canvas.drawPath(trackPath, haloPaint)
            canvas.drawPath(trackPath, trackPaint)
        }

        projected.forEachIndexed { index, point ->
            val isSelected = index == selectedIndex
            discPaint.color = surface
            canvas.drawCircle(point.x, point.y, pointRadius, discPaint)
            ringPaint.color = if (isSelected) danger else accent
            canvas.drawCircle(point.x, point.y, pointRadius, ringPaint)

            val label = (index + 1).toString()
            labelPaint.getTextBounds(label, 0, label.length, textBounds)
            canvas.drawText(label, point.x, point.y + textBounds.height() / 2F, labelPaint)
        }

        drawScaleBar(canvas)
    }

    /** A bar of a round number of metres, so distances can be read off the drawing. */
    private fun drawScaleBar(canvas: Canvas) {
        if (projected.size < 2) return
        val maxPixels = width * SCALE_BAR_MAX_FRACTION
        val rawMeters = maxPixels * metersPerPixel
        if (rawMeters <= 0 || rawMeters.isNaN()) return

        val magnitude = 10.0.pow(log10(rawMeters).toInt().toDouble())
        val niceMeters = listOf(5.0, 2.0, 1.0)
            .map { it * magnitude }
            .firstOrNull { it <= rawMeters } ?: magnitude
        val barPixels = (niceMeters / metersPerPixel).toFloat()
        if (barPixels < 1F || barPixels > width) return

        val left = padding
        val bottom = height - padding / 2F
        canvas.drawLine(left, bottom, left + barPixels, bottom, scalePaint)
        canvas.drawLine(left, bottom - 5F.dp(), left, bottom, scalePaint)
        canvas.drawLine(left + barPixels, bottom - 5F.dp(), left + barPixels, bottom, scalePaint)

        val text = if (niceMeters >= 1000) {
            context.getString(R.string.route_scale_km, niceMeters / 1000)
        } else {
            context.getString(R.string.route_scale_m, niceMeters.toInt())
        }
        canvas.drawText(text, left, bottom - 8F.dp(), scaleTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP || projected.isEmpty()) {
            return super.onTouchEvent(event)
        }
        val nearest = projected.indices.minByOrNull {
            hypot(projected[it].x - event.x, projected[it].y - event.y)
        } ?: return super.onTouchEvent(event)

        val distance = hypot(projected[nearest].x - event.x, projected[nearest].y - event.y)
        if (distance <= pointRadius + touchSlop) {
            selectedIndex = nearest
            onPointSelected?.invoke(nearest)
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    private fun Float.dp() =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics)

    private fun Float.sp() =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, resources.displayMetrics)
}
