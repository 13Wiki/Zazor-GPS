package com.gps.zazor.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.gps.zazor.R
import com.gps.zazor.databinding.ViewNotesDragBinding
import com.gps.zazor.utils.extensions.goneIfEmpty
import com.gps.zazor.utils.extensions.showOrHide
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.show
import com.gps.zazor.utils.viewBinding.viewBinding
import kotlinx.android.synthetic.main.view_notes_drag.view.*

class NotesDragView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    companion object {

        private const val COORDINATES_THRESHOLD = 100
    }

    private val binding by viewBinding(ViewNotesDragBinding::bind)

    private var xDelta = 0
    private var yDelta = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.view_notes_drag, this)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {

        ev?.let {
            return (ev.x >= binding.llNotesContainer.left && ev.x <= binding.llNotesContainer.right
                    && ev.y >= binding.llNotesContainer.top && ev.y <= binding.llNotesContainer.bottom)
        } ?: return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { ev ->
            val x = ev.x.toInt()
            val y = ev.y.toInt()
            if (ev.x >= binding.llNotesContainer.left && ev.x <= binding.llNotesContainer.right
                && ev.y >= binding.llNotesContainer.top && ev.y <= binding.llNotesContainer.bottom) {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        (binding.llNotesContainer.layoutParams as? LayoutParams)?.run {
                            xDelta = x - leftMargin
                            yDelta = y - topMargin
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (y - yDelta > COORDINATES_THRESHOLD
                            && y - yDelta + llNotesContainer.height < this.height - COORDINATES_THRESHOLD
                            && x - xDelta > COORDINATES_THRESHOLD
                            && x - xDelta + llNotesContainer.width < this.width - COORDINATES_THRESHOLD) {
                            binding.llNotesContainer.setMargins(x - xDelta, y - yDelta)
                        }
                    }
                }
            }
        }
        invalidate()
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
            tvAccuracy.goneIfEmpty(accuracy?.let {
                context.getString(R.string.accuracy, it)
            })
            tvNote.goneIfEmpty(notes)
        }
    }

    fun hide() {
        binding.llNotesContainer.hide()
    }

    private fun View.setMargins(left: Int, top: Int) {
        (layoutParams as? LayoutParams)?.let { lp ->
            lp.leftMargin = left
            lp.topMargin = top
            layoutParams = lp
        }
    }
}