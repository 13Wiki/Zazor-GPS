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
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max

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
        private const val COMPACT_PADDING_DP = 18F
        /** Radii to the middle of the ring: half the drawn disc, less half its stroke. */
        private const val COMPACT_POINT_DP = 3.75F
        private const val POINT_RADIUS_DP = 12.5F
        private const val COMPACT_RING_DP = 2.5F
        private const val RING_DP = 3F
        private const val COMPACT_TRACK_DP = 3F
        private const val TRACK_WIDTH_DP = 4F
        private const val TOUCH_SLOP_DP = 24F
        /** Metres of span assumed when every shot was taken from one spot. */
        private const val SINGLE_POINT_SPAN_M = 60.0
    }

    private val accent = ContextCompat.getColor(context, R.color.ds_accent)
    private val danger = ContextCompat.getColor(context, R.color.ds_danger)
    private val ink = ContextCompat.getColor(context, R.color.ds_text_primary)
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
    private val padding get() = if (isCompact) COMPACT_PADDING_DP.dp() else PADDING_DP.dp()
    private val pointRadius get() = if (isCompact) COMPACT_POINT_DP.dp() else POINT_RADIUS_DP.dp()
    private val trackWidth get() = if (isCompact) COMPACT_TRACK_DP.dp() else TRACK_WIDTH_DP.dp()
    private val ringWidth get() = if (isCompact) COMPACT_RING_DP.dp() else RING_DP.dp()
    private val touchSlop = TOUCH_SLOP_DP.dp()

    private val projected = mutableListOf<PointF>()
    private val trackPath = Path()
    private val textBounds = Rect()

    /** Located photos only, oldest first - the order the track is walked in. */
    private var points: List<Photo> = emptyList()

    var selectedIndex: Int = -1
        set(value) {
            field = value
            invalidate()
        }

    /** Invoked with the index into the located points, or -1 when the tap missed. */
    var onPointSelected: ((Int) -> Unit)? = null

    /**
     * Invoked whenever the points have been laid out afresh. Anything pinned to a drawn point -
     * the map screen's photo callout - repositions itself from here.
     */
    var onProjected: (() -> Unit)? = null

    /**
     * Leaves the chosen point undrawn, for a screen that marks it with something of its own and
     * would otherwise show a disc peeking out from behind it.
     */
    var hidesSelected: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Room kept clear at the top and bottom, in pixels, for whatever floats over the drawing. The
     * track is fitted into what is left, so a bottom sheet never sits on top of half the walk.
     */
    var topInset: Float = 0F
        set(value) {
            field = value
            project()
            invalidate()
        }

    var bottomInset: Float = 0F
        set(value) {
            field = value
            project()
            invalidate()
        }

    /**
     * A thumbnail of the track rather than a map to work on: smaller points, no scale bar and no
     * touch. It is what a day's card in the outings list carries.
     */
    var isCompact: Boolean = false
        set(value) {
            field = value
            isClickable = !value
            requestLayout()
            invalidate()
        }

    init {
        labelPaint.textSize = 12F.sp()
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

    /** Where a point sits inside the view, for a callout pinned to it. */
    fun pointPosition(index: Int): PointF? = projected.getOrNull(index)

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
        val usableH = (height - padding * 2 - topInset - bottomInset).coerceAtLeast(1F)

        // A single point, or several from one spot, has no span: centre it at a fixed zoom.
        val degreesPerMeter = 1.0 / 111_320.0
        val fallbackSpan = SINGLE_POINT_SPAN_M * degreesPerMeter
        val effectiveSpanX = max(spanX, fallbackSpan)
        val effectiveSpanY = max(spanY, fallbackSpan)

        val scale = minOf(usableW / effectiveSpanX, usableH / effectiveSpanY)

        val centreX = (xs.max() + xs.min()) / 2
        val centreY = (ys.max() + ys.min()) / 2

        // The middle of what is left once the floating chrome has had its share.
        val midY = (height + topInset - bottomInset) / 2F

        points.indices.forEach { i ->
            val x = (width / 2F + ((xs[i] - centreX) * scale)).toFloat()
            val y = (midY + ((ys[i] - centreY) * scale)).toFloat()
            projected.add(PointF(x, y))
            if (i == 0) trackPath.moveTo(x, y) else trackPath.lineTo(x, y)
        }

        onProjected?.invoke()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (projected.isEmpty()) return

        trackPaint.strokeWidth = trackWidth
        haloPaint.strokeWidth = trackWidth * 4
        ringPaint.strokeWidth = ringWidth
        if (projected.size > 1) {
            canvas.drawPath(trackPath, haloPaint)
            canvas.drawPath(trackPath, trackPaint)
        }

        projected.forEachIndexed { index, point ->
            val isSelected = index == selectedIndex && !isCompact
            if (isSelected && hidesSelected) return@forEachIndexed
            // Where the walking stopped, filled rather than hollow, as the design ends a track.
            if (isCompact && index == projected.lastIndex && projected.size > 1) {
                discPaint.color = danger
                canvas.drawCircle(point.x, point.y, pointRadius + ringWidth / 2F, discPaint)
                ringPaint.color = surface
                canvas.drawCircle(point.x, point.y, pointRadius + ringWidth, ringPaint)
                return@forEachIndexed
            }
            discPaint.color = surface
            canvas.drawCircle(point.x, point.y, pointRadius, discPaint)
            ringPaint.color = if (isSelected) danger else accent
            canvas.drawCircle(point.x, point.y, pointRadius, ringPaint)

            // A card's points are too small to number, and nothing there is chosen anyway.
            if (isCompact) return@forEachIndexed
            val label = (index + 1).toString()
            labelPaint.getTextBounds(label, 0, label.length, textBounds)
            canvas.drawText(label, point.x, point.y + textBounds.height() / 2F, labelPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isCompact) return false
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
