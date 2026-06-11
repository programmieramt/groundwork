package com.groundwork.programmieramt.ui.team

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groundwork.programmieramt.databinding.ItemTeamMemberBinding
import com.groundwork.programmieramt.db.entity.TeamMemberEntity

class TeamMemberAdapter(
    private val onClick: (TeamMemberEntity) -> Unit,
    private val onLongClick: (TeamMemberEntity) -> Unit
) : ListAdapter<TeamMemberEntity, TeamMemberAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemTeamMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: TeamMemberEntity) {
            binding.tvName.text = entity.name
            binding.tvRolle.text = entity.rolle
            binding.root.setOnClickListener { onClick(entity) }
            binding.root.setOnLongClickListener { onLongClick(entity); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTeamMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TeamMemberEntity>() {
            override fun areItemsTheSame(a: TeamMemberEntity, b: TeamMemberEntity) = a.id == b.id
            override fun areContentsTheSame(a: TeamMemberEntity, b: TeamMemberEntity) = a == b
        }
    }
}
