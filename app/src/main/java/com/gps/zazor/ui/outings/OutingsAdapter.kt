package com.gps.zazor.ui.outings

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gps.zazor.R
import com.gps.zazor.data.models.Outing
import com.gps.zazor.databinding.ItemOutingBinding
import com.gps.zazor.utils.Formats
import com.gps.zazor.utils.time.PhotoClock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A card per day: that day's track drawn small, what it adds up to, and the two things worth
 * doing with it. The card itself opens the day's map.
 */
class OutingsAdapter(
    private val onClick: (Outing) -> Unit,
    private val onDelete: (Outing) -> Unit,
    private val onShare: (Outing) -> Unit
) : ListAdapter<Outing, OutingsAdapter.OutingHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        OutingHolder(ItemOutingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: OutingHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OutingHolder(private val binding: ItemOutingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(outing: Outing) {
            val context = binding.root.context
            binding.tvDay.text = outing.date.label(context.resources)
            binding.tvSummary.text = context.getString(
                R.string.outing_summary,
                context.resources.getQuantityString(
                    R.plurals.outing_points, outing.pointCount, outing.pointCount
                ),
                Formats.distance(context, outing.distanceMeters)
            )
            // The hours walked, and where - whichever of the two the day actually carries.
            binding.tvRange.text = listOfNotNull(
                outing.startedAt?.let { start ->
                    outing.finishedAt?.let { end ->
                        context.getString(
                            R.string.outing_time_range,
                            PhotoClock.formatTime(start),
                            PhotoClock.formatTime(end)
                        )
                    }
                },
                outing.photos.firstNotNullOfOrNull { it.address?.takeIf(String::isNotBlank) }
            ).joinToString(" · ")

            binding.vRoute.isCompact = true
            binding.vRoute.setPhotos(outing.photos)

            // Accent on the day still being walked, plain on the days already closed.
            binding.ivShare.setBackgroundResource(
                if (outing.date == LocalDate.now()) R.drawable.ds_tile_button_accent
                else R.drawable.ds_tile_button
            )

            binding.clRoot.setOnClickListener { onClick(outing) }
            binding.ivDelete.setOnClickListener { onDelete(outing) }
            binding.ivShare.setOnClickListener { onShare(outing) }
        }
    }

    private fun LocalDate.label(resources: Resources): String = when (this) {
        LocalDate.now() -> resources.getString(R.string.outing_today)
        LocalDate.now().minusDays(1) -> resources.getString(R.string.outing_yesterday)
        else -> format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
    }

    private companion object {

        val DIFF = object : DiffUtil.ItemCallback<Outing>() {

            override fun areItemsTheSame(old: Outing, new: Outing) = old.date == new.date

            /** Compared element by element: the diffing lint check cannot see through List.equals. */
            override fun areContentsTheSame(old: Outing, new: Outing) =
                old.photos.size == new.photos.size &&
                    old.photos.indices.all { old.photos[it].path == new.photos[it].path }
        }
    }
}
