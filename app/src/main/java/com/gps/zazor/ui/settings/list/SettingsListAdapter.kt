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
import com.gps.zazor.data.models.MainSetting
import com.gps.zazor.data.models.MainSettingType
import com.gps.zazor.utils.extensions.show

class SettingsListAdapter(private val settings: List<MainSetting>,
                          private val onItemClick: (MainSettingType) -> Unit) : RecyclerView.Adapter<SettingsListAdapter.SettingHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingHolder =
        SettingHolder.newInstance(parent, onItemClick)

    override fun onBindViewHolder(holder: SettingHolder, position: Int) {
        holder.bind(settings[position])
    }

    override fun getItemCount(): Int = settings.size

    class SettingHolder(view: View,
                        private val onItemClick: (MainSettingType) -> Unit) : RecyclerView.ViewHolder(view) {

        companion object {

            fun newInstance(parent: ViewGroup, onItemClick: (MainSettingType) -> Unit) =
                SettingHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_setting, parent, false), onItemClick)
        }

        fun bind(setting: MainSetting) {
            itemView.run {
                findViewById<TextView>(R.id.tvTitle).text = context.getString(setting.stringRes)
                setOnClickListener {
                    onItemClick(setting.type)
                }
                setting.isChecked?.let {
                    findViewById<SwitchCompat>(R.id.sValue).run {
                        show()
                        isChecked = it
                    }
                    findViewById<ImageView>(R.id.ivArrow).isVisible = false
                } ?: run {
                    findViewById<ImageView>(R.id.ivArrow).isVisible = true
                }
            }
        }
    }
}