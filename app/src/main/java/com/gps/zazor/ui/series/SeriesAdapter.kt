package com.gps.zazor.ui.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gps.zazor.R
import com.gps.zazor.data.models.CoordinateFormat
import com.gps.zazor.data.models.Photo
import com.gps.zazor.databinding.ItemSeriesFrameBinding
import com.gps.zazor.utils.extensions.loadImage
import com.gps.zazor.utils.time.PhotoClock

/**
 * The frames of one approach, oldest first: the order the person walked in.
 *
 * @param bestPath the frame the series takes its coordinate from, badged for the receiver.
 */
class SeriesAdapter(
    private val coordinateFormat: CoordinateFormat,
    private val onClick: (Photo) -> Unit
) : ListAdapter<Photo, SeriesAdapter.FrameHolder>(DIFF) {

    var bestPath: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FrameHolder(ItemSeriesFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: FrameHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class FrameHolder(private val binding: ItemSeriesFrameBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Photo, position: Int) {
            val context = binding.root.context
            binding.ivPreview.loadImage(photo.path, circle = false)
            binding.tvTitle.text = photo.name.ifBlank {
                context.getString(R.string.series_frame_number, position + 1)
            }
            val hasPosition = photo.lat != null && photo.lng != null
            binding.tvCoordinates.isVisible = hasPosition
            if (hasPosition) {
                binding.tvCoordinates.text =
                    "${coordinateFormat.format(photo.lat!!)}, ${coordinateFormat.format(photo.lng!!)}"
            }
            val time = PhotoClock.formatTime(photo.date)
            // Accuracy is the whole reason a series exists, so a frame without one says so rather
            // than leaving a blank line where the number should be.
            binding.tvAccuracy.text = photo.accuracyMeters?.let {
                context.getString(R.string.series_frame_meta, Math.round(it), time)
            } ?: context.getString(R.string.series_frame_no_fix, time)
            binding.tvBest.isVisible = photo.path == bestPath
            binding.root.setOnClickListener { onClick(photo) }
        }
    }

    private companion object {

        val DIFF = object : DiffUtil.ItemCallback<Photo>() {

            override fun areItemsTheSame(old: Photo, new: Photo) = old.path == new.path

            override fun areContentsTheSame(old: Photo, new: Photo) = old == new
        }
    }
}
