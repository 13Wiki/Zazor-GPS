package com.gps.zazor.ui.outings

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

class OutingsAdapter(
    private val onClick: (Outing) -> Unit
) : ListAdapter<Outing, OutingsAdapter.OutingHolder>(DIFF) {

    var selectedDate: LocalDate? = null
        set(value) {
            val previous = field
            field = value
            // Repaint only the two rows whose selection changed, not the whole strip.
            listOf(previous, value).forEach { date ->
                currentList.indexOfFirst { it.date == date }
                    .takeIf { it >= 0 }
                    ?.let(::notifyItemChanged)
            }
        }

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
                outing.pointCount,
                Formats.distance(context, outing.distanceMeters)
            )
            binding.tvRange.text = outing.startedAt?.let { start ->
                outing.finishedAt?.let { end ->
                    context.getString(
                        R.string.outing_time_range,
                        PhotoClock.formatTime(start),
                        PhotoClock.formatTime(end)
                    )
                }
            }.orEmpty()
            binding.vSelected.visibility =
                if (outing.date == selectedDate) android.view.View.VISIBLE
                else android.view.View.INVISIBLE
            binding.root.setOnClickListener { onClick(outing) }
        }
    }

    private fun LocalDate.label(resources: android.content.res.Resources): String = when (this) {
        LocalDate.now() -> resources.getString(R.string.outing_today)
        LocalDate.now().minusDays(1) -> resources.getString(R.string.outing_yesterday)
        else -> format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
    }

    private companion object {

        val DIFF = object : DiffUtil.ItemCallback<Outing>() {

            override fun areItemsTheSame(old: Outing, new: Outing) = old.date == new.date

            override fun areContentsTheSame(old: Outing, new: Outing) =
                old.photos.map { it.path } == new.photos.map { it.path }
        }
    }
}
