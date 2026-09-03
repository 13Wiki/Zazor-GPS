package com.gps.zazor.ui.media.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.gps.zazor.data.models.Photo
import com.gps.zazor.databinding.ItemMediaBinding
import com.gps.zazor.utils.time.PhotoClock
import com.gps.zazor.utils.extensions.loadImage

class MediaListAdapter(
    photos: List<Photo>,
    private val onClick: (Photo) -> Unit,
    private val onCheckListener: (Photo, Boolean) -> Unit,
    private val onShareClick: (Photo) -> Unit,
    private val onLongPressListener: () -> Unit
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

    override fun onBindViewHolder(item: Photo, viewHolder: MediaHolder, position: Int) {
        viewHolder.bind(item, isSelectableMode, selected.contains(item.path))
    }

    fun submit(photos: List<Photo>) {
        selected.retainAll(photos.map { it.path }.toSet())
        dataSet = photos
    }

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
                tvLocation.isVisible = photo.lat != null && photo.lng != null
                tvLocation.text = root.context.getString(
                    com.gps.zazor.R.string.location_pattern,
                    photo.lat?.formatCoordinate().orEmpty(),
                    photo.lng?.formatCoordinate().orEmpty()
                )
                tvAddress.isVisible = photo.address?.isNotBlank() == true
                tvAddress.text = photo.address
                tvDate.text = PhotoClock.formatDateTime(photo.date)
                ivShare.setOnClickListener { onShareClick(photo) }
                clPhoto.setOnClickListener { onClick(photo) }
                clPhoto.setOnLongClickListener {
                    onLongPressListener()
                    true
                }
            }
        }

        private fun Double.formatCoordinate() = String.format(java.util.Locale.US, "%.6f", this)
    }
}
