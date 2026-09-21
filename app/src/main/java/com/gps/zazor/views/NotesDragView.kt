package com.gps.zazor.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import com.gps.zazor.databinding.ViewNotesDragBinding
import com.gps.zazor.utils.extensions.goneIfEmpty
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.show

/**
 * The stamp (coordinates / date / time / accuracy / heading / note) burned into the photo.
 *
 * It starts flush in the bottom-left corner of the frame, where it covers the ground rather than
 * whatever was photographed, and it can be dragged anywhere on the frame from there.
 *
 * Position is a translation, not a margin: the corner can only be worked out once the stamp has
 * been measured, which is during layout, and changing layout parameters in the middle of a layout
 * pass is ignored - the stamp ended up placed below the frame, clipped away and missing from the
 * saved file.
 */
class NotesDragView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    // Inflated eagerly and bound once: the view-binding delegate needed a lifecycle owner that a
    // plain custom view does not have.
    private val binding =
        ViewNotesDragBinding.inflate(LayoutInflater.from(context), this, true)

    private var xDelta = 0F
    private var yDelta = 0F

    /**
     * Set the moment the person drags the stamp anywhere.
     *
     * Until then the stamp is re-pinned on every layout, because its height changes as lines
     * appear and disappear - a note is typed, a heading arrives - and a stamp placed once would
     * drift up the frame as it grows.
     */
    private var movedByUser = false

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!movedByUser) pinToCorner()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean =
        ev?.let(::isInsideNotes) ?: false

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val ev = event ?: return false
        if (!isInsideNotes(ev)) return false
        val container = binding.llNotesContainer
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                xDelta = ev.x - container.translationX
                yDelta = ev.y - container.translationY
            }
            MotionEvent.ACTION_MOVE -> {
                movedByUser = true
                // Clamped to the frame rather than kept clear of its edges: a corner is exactly
                // where a stamp belongs, and the old margin of a hundred pixels made the four
                // corners the only places it could not be put.
                container.translationX =
                    (ev.x - xDelta).coerceIn(0F, (width - container.width).coerceAtLeast(0).toFloat())
                container.translationY =
                    (ev.y - yDelta).coerceIn(0F, (height - container.height).coerceAtLeast(0).toFloat())
            }
        }
        return true
    }

    fun addNotes(notes: String?,
                 lat: String?,
                 long: String?,
                 date: String?,
                 time: String?,
                 accuracy: String?,
                 bearing: String? = null) {
        binding.run {
            llNotesContainer.show()
            tvLat.goneIfEmpty(lat)
            tvLong.goneIfEmpty(long)
            tvDate.goneIfEmpty(date)
            tvTime.goneIfEmpty(time)
            tvAccuracy.goneIfEmpty(accuracy?.let { context.getString(com.gps.zazor.R.string.accuracy, it) })
            tvBearing.goneIfEmpty(bearing)
            tvNote.goneIfEmpty(notes)
        }
    }

    fun hide() {
        binding.llNotesContainer.hide()
    }

    /** Bottom-left, flush with the frame. */
    private fun pinToCorner() {
        val container = binding.llNotesContainer
        container.translationX = 0F
        container.translationY = (height - container.height).coerceAtLeast(0).toFloat()
    }

    /** Hit-tested against where the stamp is drawn, which is its layout position plus the drag. */
    private fun isInsideNotes(ev: MotionEvent): Boolean =
        binding.llNotesContainer.run {
            ev.x >= x && ev.x <= x + width && ev.y >= y && ev.y <= y + height
        }
}
