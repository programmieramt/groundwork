package com.groundwork.programmieramt.ui.teamnote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groundwork.programmieramt.databinding.ItemTeamNoteBinding
import com.groundwork.programmieramt.db.entity.TeamNoteEntity
import com.groundwork.programmieramt.util.toGermanDate

class TeamNoteAdapter(
    private val onClick: (TeamNoteEntity) -> Unit
) : ListAdapter<TeamNoteEntity, TeamNoteAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemTeamNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: TeamNoteEntity) {
            binding.tvDatum.text = entity.datum.toGermanDate()
            binding.tvKontext.text = entity.kontextMeeting.ifBlank { "—" }
            binding.tvPreview.text = entity.beobachtungen.ifBlank { entity.stimmungDynamik }.ifBlank { "—" }
            binding.root.setOnClickListener { onClick(entity) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTeamNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TeamNoteEntity>() {
            override fun areItemsTheSame(a: TeamNoteEntity, b: TeamNoteEntity) = a.id == b.id
            override fun areContentsTheSame(a: TeamNoteEntity, b: TeamNoteEntity) = a == b
        }
    }
}
