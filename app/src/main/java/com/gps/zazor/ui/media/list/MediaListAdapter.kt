package com.gps.zazor.ui.media.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.gps.zazor.data.models.CoordinateFormat
import com.gps.zazor.data.models.Photo
import com.gps.zazor.databinding.ItemMediaBinding
import com.gps.zazor.utils.time.PhotoClock
import com.gps.zazor.utils.extensions.loadImage

class MediaListAdapter(
    photos: List<Photo>,
    private val onClick: (Photo) -> Unit,
    private val onCheckListener: (Photo, Boolean) -> Unit,
    private val onShareClick: (Photo) -> Unit,
    private val onLongPressListener: () -> Unit,
    private val onVoiceNoteClick: (Photo) -> Unit,
    /** The same way the coordinates are written on the picture itself. */
    private val coordinateFormat: CoordinateFormat
) : DragDropSwipeAdapter<Photo, MediaListAdapter.MediaHolder>(photos) {

    var isSelectableMode: Boolean = false
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    /** Photos checked while in selection mode, kept here so they survive a rebind. */
    private val selected = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaHolder =
        MediaHolder(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    // Called by the library when it re-creates a holder for a swiped row. It used to be `TODO()`,
    // which throws NotImplementedError.
    override fun getViewHolder(itemView: View): MediaHolder =
        MediaHolder(ItemMediaBinding.bind(itemView))

    override fun getViewToTouchToStartDraggingItem(
        item: Photo,
        viewHolder: MediaHolder,
        position: Int
    ): View = viewHolder.itemView

    /**
     * Rows are never reordered: the gallery is ordered by time, and the list disables dragging both
     * up and down anyway. Saying so here is what makes the card usable - while an item claims to be
     * draggable, the library's touch listener swallows ACTION_DOWN on the whole row to start a drag
     * that can never happen, so the card's own tap (open the photo) and long press (selection mode)
     * never fired at all.
     */
    override fun canBeDragged(item: Photo, viewHolder: MediaHolder, position: Int): Boolean = false

    override fun onBindViewHolder(item: Photo, viewHolder: MediaHolder, position: Int) {
        viewHolder.bind(item, isSelectableMode, selected.contains(item.path))
    }

    fun submit(photos: List<Photo>) {
        selected.retainAll(photos.map { it.path }.toSet())
        dataSet = photos
    }

    /** What the confirmation dialog puts in front of the user before it wipes anything. */
    val selectedCount: Int get() = selected.size

    fun clearSelection() {
        selected.clear()
        isSelectableMode = false
    }

    inner class MediaHolder(private val binding: ItemMediaBinding) :
        DragDropSwipeAdapter.ViewHolder(binding.root) {

        fun bind(photo: Photo, isSelectableMode: Boolean, isChecked: Boolean) {
            binding.run {
                cbSelect.isVisible = isSelectableMode
                // Detach the listener before setting the state, otherwise recycling a row fires
                // a spurious check callback for whatever photo lands in it.
                cbSelect.setOnCheckedChangeListener(null)
                cbSelect.isChecked = isChecked
                cbSelect.setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(photo.path) else selected.remove(photo.path)
                    onCheckListener(photo, checked)
                }
                ivPreview.loadImage(photo.path, circle = false)
                val hasPosition = photo.lat != null && photo.lng != null
                // The note the person typed is what this photo is called; a date is what it is
                // called when they did not type one.
                val date = PhotoClock.formatDateTime(photo.date)
                tvName.text = photo.name.ifBlank { date }
                // Same coordinates as on the picture itself, legible over the thumbnail. They are
                // not repeated under it: one place to read them is enough.
                tvPreviewCoordinates.isVisible = hasPosition
                tvPreviewCoordinates.text = if (hasPosition) {
                    "${photo.lat?.formatCoordinate()}, ${photo.lng?.formatCoordinate()}"
                } else {
                    ""
                }
                // A direction is recorded for every frame but shown for the wide ones: those are
                // the shots someone has to stand in the same place to repeat.
                val bearing = photo.bearingDegrees.takeIf { photo.isWide }
                tvBearing.isVisible = bearing != null
                tvBearing.text = bearing?.let {
                    root.context.getString(com.gps.zazor.R.string.bearing_short, Math.round(it) % 360)
                }.orEmpty()
                // One line under the title, as in the design: when, then where. The date is left
                // out when it already is the title.
                tvDate.text = listOfNotNull(
                    date.takeIf { photo.name.isNotBlank() },
                    photo.address?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                tvDate.isVisible = tvDate.text.isNotEmpty()
                // The voice button appears only for photos that actually carry a recording.
                ivVoiceNote.isVisible = photo.voiceNotePath != null
                ivVoiceNote.setOnClickListener { onVoiceNoteClick(photo) }
                ivShare.setOnClickListener { onShareClick(photo) }
                clPhoto.setOnClickListener { onClick(photo) }
                clPhoto.setOnLongClickListener {
                    onLongPressListener()
                    true
                }
            }
        }

        private fun Double.formatCoordinate() = coordinateFormat.format(this)
    }
}
