package com.gps.zazor.ui.settings.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gps.zazor.R
import com.gps.zazor.data.models.MainSettingType
import com.gps.zazor.data.models.SettingRow

class SettingsListAdapter(
    private val rows: List<SettingRow>,
    private val onItemClick: (MainSettingType) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {

        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is SettingRow.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.item_setting_header, parent, false))
        } else {
            SettingHolder(inflater.inflate(R.layout.item_setting, parent, false), onItemClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is SettingRow.Header -> (holder as HeaderHolder).bind(row)
            is SettingRow.Item -> (holder as SettingHolder).bind(row)
        }
    }

    override fun getItemCount(): Int = rows.size

    class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(header: SettingRow.Header) {
            (itemView as TextView).setText(header.titleRes)
        }
    }

    class SettingHolder(
        view: View,
        private val onItemClick: (MainSettingType) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        fun bind(item: SettingRow.Item) {
            itemView.run {
                findViewById<ImageView>(R.id.ivIcon).setImageResource(item.iconRes)
                findViewById<TextView>(R.id.tvTitle).setText(item.titleRes)
                findViewById<TextView>(R.id.tvSubtitle).run {
                    isVisible = item.subtitleRes != null
                    item.subtitleRes?.let(::setText)
                }
                // A switch and a value would say the same thing twice, so a row shows one or the
                // other: the switch when the setting is on or off, the value when it is a choice.
                findViewById<SwitchCompat>(R.id.sValue).run {
                    isVisible = item.isChecked != null
                    isChecked = item.isChecked ?: false
                }
                findViewById<TextView>(R.id.tvValue).run {
                    isVisible = item.value != null
                    text = item.value
                }
                findViewById<ImageView>(R.id.ivArrow).isVisible = item.isChecked == null
                setOnClickListener { onItemClick(item.type) }
            }
        }
    }
}
