package com.gps.zazor.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.gps.zazor.databinding.ViewNotesDragBinding
import com.gps.zazor.utils.extensions.goneIfEmpty
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.show

/**
 * The stamp (coordinates / date / time / accuracy / note) the user can drag around the photo.
 */
class NotesDragView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {

        private const val COORDINATES_THRESHOLD = 100
    }

    // Inflated eagerly and bound once: the view-binding delegate needed a lifecycle owner that a
    // plain custom view does not have.
    private val binding =
        ViewNotesDragBinding.inflate(LayoutInflater.from(context), this, true)

    private var xDelta = 0
    private var yDelta = 0

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean =
        ev?.let(::isInsideNotes) ?: false

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val ev = event ?: return false
        if (!isInsideNotes(ev)) return false
        val x = ev.x.toInt()
        val y = ev.y.toInt()
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                (binding.llNotesContainer.layoutParams as? LayoutParams)?.run {
                    xDelta = x - leftMargin
                    yDelta = y - topMargin
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val container = binding.llNotesContainer
                if (y - yDelta > COORDINATES_THRESHOLD
                    && y - yDelta + container.height < height - COORDINATES_THRESHOLD
                    && x - xDelta > COORDINATES_THRESHOLD
                    && x - xDelta + container.width < width - COORDINATES_THRESHOLD
                ) {
                    container.setMargins(x - xDelta, y - yDelta)
                }
            }
        }
        return true
    }

    fun addNotes(notes: String?,
                 lat: String?,
                 long: String?,
                 date: String?,
                 time: String?,
                 accuracy: String?) {
        binding.run {
            llNotesContainer.show()
            tvLat.goneIfEmpty(lat)
            tvLong.goneIfEmpty(long)
            tvDate.goneIfEmpty(date)
            tvTime.goneIfEmpty(time)
            tvAccuracy.goneIfEmpty(accuracy?.let { context.getString(com.gps.zazor.R.string.accuracy, it) })
            tvNote.goneIfEmpty(notes)
        }
    }

    fun hide() {
        binding.llNotesContainer.hide()
    }

    private fun isInsideNotes(ev: MotionEvent): Boolean =
        binding.llNotesContainer.run {
            ev.x >= left && ev.x <= right && ev.y >= top && ev.y <= bottom
        }

    private fun View.setMargins(left: Int, top: Int) {
        (layoutParams as? LayoutParams)?.let { lp ->
            lp.leftMargin = left
            lp.topMargin = top
            layoutParams = lp
        }
    }
}
