package com.groundwork.programmieramt.ui.sofort

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groundwork.programmieramt.databinding.ItemSofortNoteBinding
import com.groundwork.programmieramt.db.entity.SofortNoteEntity
import com.groundwork.programmieramt.util.toGermanDate

class SofortAdapter(
    private val onClick: (SofortNoteEntity) -> Unit,
    private val onLongClick: (SofortNoteEntity) -> Unit
) : ListAdapter<SofortNoteEntity, SofortAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemSofortNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: SofortNoteEntity) {
            binding.tvDatum.text = entity.datum.toGermanDate()
            binding.tvKategorie.text = entity.kategorie
            binding.tvCapturePreview.text = "—"
            binding.root.setOnClickListener { onClick(entity) }
            binding.root.setOnLongClickListener { onLongClick(entity); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemSofortNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SofortNoteEntity>() {
            override fun areItemsTheSame(a: SofortNoteEntity, b: SofortNoteEntity) = a.id == b.id
            override fun areContentsTheSame(a: SofortNoteEntity, b: SofortNoteEntity) = a == b
        }
    }
}
