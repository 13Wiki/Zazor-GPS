package com.gps.zazor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.pow
import kotlin.math.sqrt

class DrawView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    /**
     * One mark, as the person drew it.
     *
     * The line and its arrow head used to live in two separate lists, which was enough to paint
     * them but not enough to take one back: nothing said which head belonged to which line, or
     * which of the two was drawn last. A mark keeps its own pieces together, and the list keeps
     * their order, so undoing is dropping the last one.
     */
    private class Mark(val path: Path, val arrowHead: Path?, val marker: PointF? = null)

    private var paint: Paint? = null

    private var arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.RED
        strokeWidth = 3F
    }

    private val marks = mutableListOf<Mark>()

    private var startX = 0F
    private var startY = 0F

    var mode = Mode.LINE

    var isPaintAllowed = true
    set(value) {
        isClickable = value
        isFocusable = value
    }

    var colorRes: Int
    get () = 0
    set(value) {
        paint?.color = value
        arrowPaint.color = value
        invalidate()
    }

    /** Called when a mark is started, so a screen can drop an instruction that is now obvious. */
    var onMarkAdded: (() -> Unit)? = null

    /** True while there is something to take back; the screen hides the button otherwise. */
    val hasMarks: Boolean get() = marks.isNotEmpty()

    /**
     * How thick a mark is drawn, in pixels on the frame. Three steps in the design; a thin line
     * is for outlining a small thing, a thick one for a mark that has to survive being resized
     * by a messenger.
     */
    var strokeWidth: Int = DEFAULT_STROKE_WIDTH
        set(value) {
            field = value
            paint?.strokeWidth = value.toFloat()
            invalidate()
        }

    init {
        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.RED
            strokeWidth = DEFAULT_STROKE_WIDTH.toFloat()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint?.let { drawPaint ->
            marks.forEach { mark ->
                canvas.drawPath(mark.path, drawPaint)
                mark.arrowHead?.let { canvas.drawPath(it, arrowPaint) }
                mark.marker?.let { drawMarker(canvas, it, drawPaint) }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event?.takeIf { isPaintAllowed }?.let {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    handleDownPress(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    handleMoveAction(event)
                }
                else -> Unit
            }
        }
        return super.dispatchTouchEvent(event)
    }

    fun clear() {
        marks.clear()
        invalidate()
    }

    /** Removes the last mark drawn. Returns false when there was nothing left to remove. */
    fun undo(): Boolean {
        if (marks.isEmpty()) return false
        marks.removeAt(marks.lastIndex)
        invalidate()
        return true
    }

    private fun handleDownPress(event: MotionEvent) {
        startX = event.x
        startY = event.y
        val path = Path().also { it.moveTo(event.x, event.y) }
        onMarkAdded?.invoke()
        marks.add(
            Mark(
                path,
                if (mode == Mode.ARROW) Path() else null,
                if (mode == Mode.MARKER) PointF(event.x, event.y) else null
            )
        )
        // A pin is placed by a tap with no movement at all, and only a move used to repaint:
        // the mark existed, and the frame did not show it until something else redrew.
        invalidate()
    }

    private fun handleMoveAction(event: MotionEvent) {
        val mark = marks.lastOrNull() ?: return
        when (mode) {
            // A pin is placed by touching, and slid into place without lifting the finger: on a
            // wide frame the exact spot is a stone among stones, and a fingertip covers it.
            Mode.MARKER -> mark.marker?.set(event.x, event.y)
            Mode.LINE -> {
                mark.path.lineTo(event.x, event.y)
            }
            Mode.CIRCLE -> {
                mark.path.run {
                    reset()
                    addCircle(
                        startX, startY,
                        sqrt(
                            (event.x - startX).pow(2)
                                    + (event.y - startY).pow(2)
                        ), Path.Direction.CW
                    )
                }
            }
            Mode.ARROW -> {
                mark.path.run {
                    reset()
                    moveTo(startX, startY)
                    lineTo(event.x, event.y)
                }
                mark.arrowHead?.let { drawArrow(it, event.x, startX, event.y, startY) }
            }
        }
        invalidate()
    }

    /**
     * A ring with a dot at its centre. Not a filled blob: the point of a marker is the pixel it
     * stands on, and the receiver has to be able to see what is under it.
     */
    private fun drawMarker(canvas: Canvas, at: PointF, drawPaint: Paint) {
        val radius = drawPaint.strokeWidth * MARKER_RADIUS_FACTOR
        canvas.drawCircle(at.x, at.y, radius, drawPaint)
        val dot = Paint(drawPaint).apply { style = Paint.Style.FILL }
        canvas.drawCircle(at.x, at.y, drawPaint.strokeWidth / 2, dot)
    }

    private fun drawArrow(head: Path, endX: Float, startX: Float, endY: Float, startY: Float) {
        head.apply {
            reset()
            val deltaX: Float = endX - startX
            val deltaY: Float = endY - startY
            val frac = 0.1.toFloat()
            val point_x_1: Float = startX + ((1 - frac) * deltaX + frac * deltaY)
            val point_y_1: Float = startY + ((1 - frac) * deltaY - frac * deltaX)
            val point_x_2: Float = endX + (endX - startX) * 0.07F
            val point_y_2: Float = endY + (endY - startY) * 0.07F
            val point_x_3: Float = startX + ((1 - frac) * deltaX - frac * deltaY)
            val point_y_3: Float = startY + ((1 - frac) * deltaY + frac * deltaX)
            setLayerPaint(arrowPaint)
            moveTo(point_x_1, point_y_1)
            lineTo(point_x_2, point_y_2)
            lineTo(point_x_3, point_y_3)
            lineTo(point_x_1, point_y_1)
            lineTo(point_x_1, point_y_1)
        }
    }
}

/** The middle of the three widths the sheet offers. */
private const val DEFAULT_STROKE_WIDTH = 24

enum class Mode {
    LINE, CIRCLE, ARROW, MARKER
}

/** How many stroke widths across the marker's ring is. */
private const val MARKER_RADIUS_FACTOR = 2F
