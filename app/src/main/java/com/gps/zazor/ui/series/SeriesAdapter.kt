package com.gps.zazor.ui.series

import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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
    /** Above this many metres a fix is drawn as a warning rather than as a good one. */
    private val accuracyThresholdMeters: Int,
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
            binding.tvIndex.text = (position + 1).toString()
            binding.tvTitle.text = photo.name.ifBlank {
                context.getString(R.string.series_frame_number, position + 1)
            }
            val hasPosition = photo.lat != null && photo.lng != null
            binding.tvCoordinates.isVisible = hasPosition
            binding.tvCoordinates.text = if (hasPosition) {
                "${coordinateFormat.format(photo.lat!!)}, ${coordinateFormat.format(photo.lng!!)}"
            } else {
                context.getString(R.string.series_frame_no_coordinates)
            }
            val isBest = photo.path == bestPath
            binding.tvBest.isVisible = isBest
            binding.root.setBackgroundResource(
                if (isBest) R.drawable.ds_frame_card_best else R.drawable.ds_frame_card
            )
            bindAccuracy(photo, context)
            binding.root.setOnClickListener { onClick(photo) }
        }

        /**
         * The fix as a bar and a number.
         *
         * A bar because "±12 m" and "±3 m" are the same length on the page and the difference
         * between them is the whole point of shooting a series; the fuller and greener the bar,
         * the more the coordinate can be trusted.
         */
        private fun bindAccuracy(photo: Photo, context: android.content.Context) {
            val accuracy = photo.accuracyMeters
            binding.vAccuracyTrack.isVisible = accuracy != null
            binding.vAccuracyFill.isVisible = accuracy != null
            if (accuracy == null) {
                binding.tvAccuracy.text = context.getString(R.string.series_frame_no_fix)
                binding.tvAccuracy.setTextColor(
                    ContextCompat.getColor(context, R.color.ds_text_faint)
                )
                return
            }
            val good = accuracy <= accuracyThresholdMeters
            val colour = ContextCompat.getColor(
                context, if (good) R.color.ds_signal_good else R.color.ds_warn
            )
            // Measured off the track rather than as a share of the row: a percentage here is a
            // percentage of the whole card, which filled the line and hid the number beside it.
            val track = context.resources.getDimensionPixelSize(R.dimen.ds_accuracy_track_width)
            binding.vAccuracyFill.updateLayoutParams {
                width = (track * fillFor(accuracy)).toInt()
            }
            binding.vAccuracyFill.backgroundTintList = ColorStateList.valueOf(colour)
            binding.tvAccuracy.setTextColor(colour)
            binding.tvAccuracy.text =
                context.getString(R.string.accuracy_short, Math.round(accuracy))
        }

        /** Full at a metre, empty at twenty; beyond that the number speaks for itself. */
        private fun fillFor(accuracy: Float): Float =
            ((EMPTY_AT_METERS - accuracy) / EMPTY_AT_METERS).coerceIn(0.08F, 1F)
    }

    private companion object {

        /** The distance at which the bar reads as empty. */
        const val EMPTY_AT_METERS = 20F

        val DIFF = object : DiffUtil.ItemCallback<Photo>() {

            override fun areItemsTheSame(old: Photo, new: Photo) = old.path == new.path

            override fun areContentsTheSame(old: Photo, new: Photo) = old == new
        }
    }
}
