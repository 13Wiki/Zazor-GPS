package com.gps.zazor.utils.colorPicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.gps.zazor.R

class CircleView : View {
    companion object {
        private const val DEFAULT_STYLE = 0
        private const val DEFAULT_STYLE_RES = 0
        private const val DEFAULT_COLOR_CIRCLE = Color.RED
        private const val DEFAULT_COLOR_BORDER = Color.WHITE
        private const val RING_WIDTH = 2F
        private const val HALF_DELIMITER = 2F
    }

    private val paint = Paint()

    var colorLap = DEFAULT_COLOR_CIRCLE
    var colorBorderline = DEFAULT_COLOR_BORDER
    var selection = false

    constructor(context: Context) : super(context) {
        init(null, DEFAULT_STYLE)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, DEFAULT_STYLE)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) = with(context.obtainStyledAttributes(attrs, R.styleable.CircleView, defStyle, DEFAULT_STYLE_RES)) {
        colorLap = getColor(R.styleable.CircleView_word_art_color_circle, DEFAULT_COLOR_CIRCLE)
        colorBorderline = getColor(R.styleable.CircleView_word_art_color_border, DEFAULT_COLOR_BORDER)
        initPaint()
        recycle()
    }

    private fun initPaint() = with(paint) {
        flags = Paint.ANTI_ALIAS_FLAG
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Keep the swatch square, sized by whichever side is actually bounded. A horizontal list
        // measures its items' width as UNSPECIFIED, which the default measure turns into 0, and
        // taking the smaller side then made every swatch 0 wide - the palette drew nothing.
        val bounded = listOf(widthMeasureSpec to measuredWidth, heightMeasureSpec to measuredHeight)
            .filter { (spec, _) -> MeasureSpec.getMode(spec) != MeasureSpec.UNSPECIFIED }
            .map { (_, size) -> size }
        val side = bounded.minOrNull() ?: resources.getDimensionPixelSize(R.dimen.ds_touch_min)
        setMeasuredDimension(side, side)
    }

    /**
     * The swatch as the design draws it: a full circle of the colour, and a white ring around the
     * one in hand with a gap of the sheet's own colour between them.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centreX = width.half()
        val centreY = height.half()
        val radius = minOf(width, height).half()
        canvas.drawColor(Color.TRANSPARENT)
        with(paint) {
            style = Paint.Style.FILL
            color = colorLap
            canvas.drawCircle(centreX, centreY, radius, this)
        }
        if (!selection) return
        with(paint) {
            style = Paint.Style.STROKE
            strokeWidth = RING_WIDTH * resources.displayMetrics.density
            color = colorBorderline
            canvas.drawCircle(centreX, centreY, radius + strokeWidth, this)
        }
    }

    private fun Float.half() = this / HALF_DELIMITER

    private fun Int.half() = this / HALF_DELIMITER
}