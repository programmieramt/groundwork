package com.groundwork.programmieramt.ui.oneonone

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.groundwork.programmieramt.databinding.ItemOneOnOneBinding
import com.groundwork.programmieramt.util.toGermanDate

class OneOnOneAdapter(
    private val onClick: (SessionWithMember) -> Unit
) : ListAdapter<SessionWithMember, OneOnOneAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemOneOnOneBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SessionWithMember) {
            binding.tvMemberName.text = item.memberName
            binding.tvDatum.text = item.session.datum.toGermanDate()
            binding.tvSessionNr.text = "Nr. ${item.session.sessionNumber}"
            binding.tvThemaPreview.text = item.session.thema.ifBlank { "—" }
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemOneOnOneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SessionWithMember>() {
            override fun areItemsTheSame(a: SessionWithMember, b: SessionWithMember) =
                a.session.id == b.session.id
            override fun areContentsTheSame(a: SessionWithMember, b: SessionWithMember) = a == b
        }
    }
}
