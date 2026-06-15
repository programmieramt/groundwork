package com.groundwork.programmieramt.ui.today

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.ItemTodayEntryBinding
import com.groundwork.programmieramt.util.toGermanTime

class TodayAdapter(
    private val onClick: (TodayEntry) -> Unit
) : ListAdapter<TodayEntry, TodayAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemTodayEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TodayEntry) {
            val (colorRes, labelRes) = when (item.type) {
                TodayEntryType.SOFORT -> R.color.template_sofort to R.string.nav_sofort
                TodayEntryType.TEAM_NOTE -> R.color.template_team_allgemein to R.string.nav_team_note
                TodayEntryType.ONE_ON_ONE -> R.color.template_oneonone to R.string.nav_one_on_one
                TodayEntryType.MEETING -> R.color.template_meeting to R.string.nav_meeting
                TodayEntryType.FREE_NOTE -> R.color.template_notiz to R.string.nav_notiz
            }
            val color = binding.root.context.getColor(colorRes)
            binding.colorBar.setBackgroundColor(color)
            binding.tvTime.text = item.time.toGermanTime()
            binding.tvTypeLabel.text = binding.root.context.getString(labelRes)
            binding.tvTypeLabel.setTextColor(color)
            binding.tvContext.text = item.contextText
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTodayEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TodayEntry>() {
            override fun areItemsTheSame(a: TodayEntry, b: TodayEntry) = a.type == b.type && a.id == b.id
            override fun areContentsTheSame(a: TodayEntry, b: TodayEntry) = a == b
        }
    }
}
