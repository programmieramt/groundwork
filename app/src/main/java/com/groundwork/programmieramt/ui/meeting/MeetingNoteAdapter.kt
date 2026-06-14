package com.groundwork.programmieramt.ui.meeting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groundwork.programmieramt.databinding.ItemMeetingNoteBinding
import com.groundwork.programmieramt.db.entity.MeetingNoteEntity
import com.groundwork.programmieramt.util.toGermanDate

class MeetingNoteAdapter(
    private val onClick: (MeetingNoteEntity) -> Unit,
    private val onLongClick: (MeetingNoteEntity) -> Unit
) : ListAdapter<MeetingNoteEntity, MeetingNoteAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemMeetingNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: MeetingNoteEntity) {
            binding.tvDatum.text = entity.datum.toGermanDate()
            binding.tvTeilnehmer.text = entity.teilnehmer.ifBlank { "—" }
            binding.root.setOnClickListener { onClick(entity) }
            binding.root.setOnLongClickListener { onLongClick(entity); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemMeetingNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MeetingNoteEntity>() {
            override fun areItemsTheSame(a: MeetingNoteEntity, b: MeetingNoteEntity) = a.id == b.id
            override fun areContentsTheSame(a: MeetingNoteEntity, b: MeetingNoteEntity) = a == b
        }
    }
}
