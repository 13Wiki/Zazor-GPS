package com.gps.zazor.ui.settings.appearance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gps.zazor.databinding.ItemAppearanceBinding
import com.gps.zazor.utils.launcher.LauncherAppearance

class AppearanceAdapter(
    private val onClick: (LauncherAppearance.Appearance) -> Unit
) : RecyclerView.Adapter<AppearanceAdapter.AppearanceHolder>() {

    private val items = LauncherAppearance.Appearance.entries

    var current: LauncherAppearance.Appearance = LauncherAppearance.Appearance.DEFAULT
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AppearanceHolder(
            ItemAppearanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: AppearanceHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class AppearanceHolder(private val binding: ItemAppearanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appearance: LauncherAppearance.Appearance) {
            binding.ivIcon.setImageResource(appearance.previewIcon)
            binding.tvName.setText(appearance.title)
            binding.ivIcon.alpha = if (appearance == current) 1F else 0.55F
            binding.root.setOnClickListener { onClick(appearance) }
        }
    }
}
