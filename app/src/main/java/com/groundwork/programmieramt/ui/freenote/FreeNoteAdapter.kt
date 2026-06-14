package com.groundwork.programmieramt.ui.freenote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groundwork.programmieramt.databinding.ItemFreeNoteBinding
import com.groundwork.programmieramt.db.entity.FreeNoteEntity
import com.groundwork.programmieramt.util.toGermanDate

class FreeNoteAdapter(
    private val onClick: (FreeNoteEntity) -> Unit,
    private val onLongClick: (FreeNoteEntity) -> Unit
) : ListAdapter<FreeNoteEntity, FreeNoteAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemFreeNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: FreeNoteEntity) {
            binding.tvDatum.text = entity.datum.toGermanDate()
            binding.root.setOnClickListener { onClick(entity) }
            binding.root.setOnLongClickListener { onLongClick(entity); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemFreeNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FreeNoteEntity>() {
            override fun areItemsTheSame(a: FreeNoteEntity, b: FreeNoteEntity) = a.id == b.id
            override fun areContentsTheSame(a: FreeNoteEntity, b: FreeNoteEntity) = a == b
        }
    }
}
