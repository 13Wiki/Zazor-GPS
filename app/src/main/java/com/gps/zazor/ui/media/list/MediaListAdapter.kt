package com.gps.zazor.ui.media.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.gps.zazor.R
import com.gps.zazor.data.models.Photo
import com.gps.zazor.utils.extensions.loadImage

class MediaListAdapter(private val photos: List<Photo>,
                       private val onClick: (Photo) -> Unit,
                       private val onCheckListener: (Photo, Boolean) -> Unit,
                       private val onShareClick: (Photo) -> Unit,
                       private val onLongPressListener: () -> Unit) : DragDropSwipeAdapter<Photo, MediaListAdapter.MediaHolder>(photos) {

    var isSelectableMode: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaHolder =
        MediaHolder.newInstance(parent, onClick, onCheckListener, onShareClick, onLongPressListener)

    override fun getViewToTouchToStartDraggingItem(
        item: Photo,
        viewHolder: MediaHolder,
        position: Int
    ): View {
        return viewHolder.itemView
    }

    override fun getViewHolder(itemView: View): MediaHolder = TODO()

    override fun onBindViewHolder(item: Photo, viewHolder: MediaHolder, position: Int) {
        viewHolder.bind(photos[position], isSelectableMode)
    }

    override fun getItemCount(): Int = photos.size

    class MediaHolder(view: View,
                      private val onClick: (Photo) -> Unit,
                      private val onCheckListener: (Photo, Boolean) -> Unit,
                      private val onShareClick: (Photo) -> Unit,
                      private val onLongPressListener: () -> Unit) : DragDropSwipeAdapter.ViewHolder(view) {

        companion object {

            fun newInstance(parent: ViewGroup,
                            onClick: (Photo) -> Unit,
                            onCheckListener: (Photo, Boolean) -> Unit,
                            onShareClick: (Photo) -> Unit,
                            onLongPressListener: () -> Unit) =
                MediaHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false),
                    onClick, onCheckListener, onShareClick, onLongPressListener)
        }

        fun bind(photo: Photo, isSelectableMode: Boolean) {
            itemView.run {
                findViewById<CheckBox>(R.id.cbSelect).run {
                    isVisible = isSelectableMode
                    setOnCheckedChangeListener { _, isChecked ->
                        onCheckListener(photo, isChecked)
                    }
                }
                findViewById<ImageView>(R.id.ivPreview).loadImage(photo.path, circle = false)
                findViewById<TextView>(R.id.tvLocation).text =
                    itemView.context.getString(R.string.location_pattern,
                    photo.lat.toString(), photo.lng.toString())
                findViewById<TextView>(R.id.tvAddress).text = photo.address
                findViewById<TextView>(R.id.tvDate).text = photo.date.toString("dd.MM.YYYY, HH:mm")
                findViewById<View>(R.id.ivShare).setOnClickListener {
                    onShareClick(photo)
                }
                findViewById<View>(R.id.clPhoto).run {
                    setOnClickListener {
                        onClick(photo)
                    }
                    setOnLongClickListener {
                        onLongPressListener()
                        true
                    }
                }
            }
        }
    }
}