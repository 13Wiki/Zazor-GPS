package com.gps.zazor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.pow
import kotlin.math.sqrt

class DrawView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var paint: Paint? = null

    private var arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.RED
        strokeWidth = 3F
    }

    private var paths = mutableListOf<Path>()

    private var arrowPaths = mutableListOf<Path>()

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

    init {
        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.RED
            strokeWidth = 24F
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint?.let { drawPaint ->
            paths.forEach { drawPath ->
                canvas.drawPath(drawPath, drawPaint)
            }
            arrowPaths.forEach { arrowPath ->
                canvas.drawPath(arrowPath, arrowPaint)
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
        paths.clear()
        arrowPaths.clear()
        invalidate()
    }

    private fun handleDownPress(event: MotionEvent) {
        when (mode) {
            Mode.LINE -> {
                startX = event.x
                startY = event.y
                paths.add(Path().also {
                    it.moveTo(event.x, event.y)
                })
            }
            Mode.CIRCLE -> {
                paths.add(Path().also {
                    it.moveTo(event.x, event.y)
                })
                startX = event.x
                startY = event.y
            }
            Mode.ARROW -> {
                startX = event.x
                startY = event.y
                paths.add(Path().also {
                    it.moveTo(event.x, event.y)
                })
                arrowPaths.add(Path())
            }
        }
    }

    private fun handleMoveAction(event: MotionEvent) {
        when (mode) {
            Mode.LINE -> {
                paths.lastOrNull()?.lineTo(event.x, event.y)
            }
            Mode.CIRCLE -> {
                paths.lastOrNull()?.run {
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
                paths.lastOrNull()?.run {
                    reset()
                    moveTo(startX, startY)
                    lineTo(event.x, event.y)
                    drawArrow(event.x, startX, event.y, startY)
                }
            }
        }
        invalidate()
    }

    private fun drawArrow(endX: Float, startX: Float, endY: Float, startY: Float) {
        arrowPaths.lastOrNull()?.apply {
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

enum class Mode {
    LINE, CIRCLE, ARROW
}