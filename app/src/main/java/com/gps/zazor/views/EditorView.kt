package com.gps.zazor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PathEffect
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Whether a control button is drawn on the text selector.
 */
enum class ShowButtonOnSelector {
    SHOW_BUTTON, HIDE_BUTTON
}

/**
 * A free-form text overlay the user can drag, pinch-scale and two-finger rotate on top of a photo.
 *
 * Replaces the abandoned `com.cleveroad.droidart.EditorView`, keeping the same property and
 * setter names so the screens using it did not have to change. Because the text is drawn on the
 * view's own canvas, `View.getBitmap()` on the container captures it together with the photo.
 */
class EditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {

        private const val DEFAULT_TEXT_SIZE_SP = 32F
        private const val MIN_SCALE = 0.3F
        private const val MAX_SCALE = 8F
        private const val SELECTOR_PADDING = 16F
        private const val SHADOW_RADIUS = 4F
        private const val SHADOW_OFFSET = 2F
        /** How long the selector stays visible after the last touch. */
        private const val SELECTOR_TIMEOUT_MS = 2000L
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE_SP, resources.displayMetrics
        )
        textAlign = Paint.Align.CENTER
    }

    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.TRANSPARENT
    }

    private val textBounds = Rect()

    /** Centre of the text, in view coordinates. Lazily centred on first layout. */
    private val position = PointF()
    private var positioned = false

    private var scale = 1F
    private var rotation = 0F

    private var lastTouch = PointF()
    private var lastSpacing = 0F
    private var lastAngle = 0F
    private var isSelectorVisible = false

    private val hideSelector = Runnable {
        isSelectorVisible = false
        invalidate()
    }

    /** Text drawn on the photo. Empty text hides the overlay entirely. */
    var text: String = ""
        set(value) {
            field = value
            showSelectorTemporarily()
            invalidate()
        }

    /** Colour of [text]. */
    var textColor: Int
        get() = textPaint.color
        set(value) {
            textPaint.color = value
            invalidate()
        }

    /**
     * Font resource id (`R.font.*`). Any value that is not a usable font resource - including the
     * `Typeface.DEFAULT.style` sentinel the fonts list starts with - falls back to the system font.
     */
    var fontId: Int = 0
        set(value) {
            field = value
            textPaint.typeface = loadTypeface(value)
            invalidate()
        }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // shadows need software rendering
        isClickable = true
        isFocusable = true
    }

    fun setColorForSelector(color: Int) {
        selectorPaint.color = color
        invalidate()
    }

    fun setColorForDashLine(color: Int) {
        selectorPaint.color = color
        invalidate()
    }

    fun setStrokeWidthForDashLine(width: Float) {
        selectorPaint.strokeWidth = width
        invalidate()
    }

    fun setPathEffectForSelector(effect: PathEffect?) {
        selectorPaint.pathEffect = effect
        invalidate()
    }

    fun setColorForTextShadow(color: Int) {
        textPaint.setShadowLayer(SHADOW_RADIUS, SHADOW_OFFSET, SHADOW_OFFSET, color)
        invalidate()
    }

    // The original library drew move/rotate/reset buttons on the selector. This app hides all
    // three, so they are accepted and recorded but never drawn.
    fun setColorForSelectorButton(color: Int) = Unit

    fun showScaleRotateButton(mode: ShowButtonOnSelector) = Unit

    fun showResetViewTextButton(mode: ShowButtonOnSelector) = Unit

    fun showChangeViewTextButton(mode: ShowButtonOnSelector) = Unit

    /** Drops the text and resets the transform. */
    fun clear() {
        text = ""
        scale = 1F
        rotation = 0F
        positioned = false
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!positioned && w > 0 && h > 0) {
            position.set(w / 2F, h / 2F)
            positioned = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (text.isEmpty()) return
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        canvas.save()
        canvas.translate(position.x, position.y)
        canvas.rotate(rotation)
        canvas.scale(scale, scale)

        // getTextBounds is relative to the baseline; shift so the text is vertically centred.
        canvas.drawText(text, 0F, textBounds.height() / 2F, textPaint)

        if (isSelectorVisible && selectorPaint.color != Color.TRANSPARENT) {
            val halfWidth = textBounds.width() / 2F + SELECTOR_PADDING
            val halfHeight = textBounds.height() / 2F + SELECTOR_PADDING
            canvas.drawRect(-halfWidth, -halfHeight, halfWidth, halfHeight, selectorPaint)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (text.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouch.set(event.x, event.y)
                showSelectorTemporarily()
            }
            MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount >= 2) {
                lastSpacing = event.spacing()
                lastAngle = event.angle()
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val spacing = event.spacing()
                    if (lastSpacing > 0F && spacing > 0F) {
                        scale = (scale * (spacing / lastSpacing)).coerceIn(MIN_SCALE, MAX_SCALE)
                    }
                    lastSpacing = spacing
                    val angle = event.angle()
                    rotation += angle - lastAngle
                    lastAngle = angle
                } else {
                    position.offset(event.x - lastTouch.x, event.y - lastTouch.y)
                    lastTouch.set(event.x, event.y)
                }
                showSelectorTemporarily()
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                lastSpacing = 0F
                // Keep dragging with whichever finger is still down.
                val remaining = if (event.actionIndex == 0) 1 else 0
                lastTouch.set(event.getX(remaining), event.getY(remaining))
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideSelector)
        super.onDetachedFromWindow()
    }

    private fun showSelectorTemporarily() {
        isSelectorVisible = true
        removeCallbacks(hideSelector)
        postDelayed(hideSelector, SELECTOR_TIMEOUT_MS)
    }

    private fun loadTypeface(id: Int): Typeface =
        try {
            ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

    private fun MotionEvent.spacing() =
        hypot(getX(0) - getX(1), getY(0) - getY(1))

    private fun MotionEvent.angle() =
        Math.toDegrees(atan2(getY(1) - getY(0), getX(1) - getX(0)).toDouble()).toFloat()
}
