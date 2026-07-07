package com.groundwork.programmieramt.ui.oneonone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentOneOnOneListBinding
import com.groundwork.programmieramt.databinding.ItemSessionBinding
import com.groundwork.programmieramt.db.entity.OneOnOneSession
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OneOnOneListFragment : Fragment() {

    private var _binding: FragmentOneOnOneListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OneOnOneViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOneOnOneListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SessionAdapter { session ->
            findNavController().navigate(R.id.action_list_to_canvas,
                OneOnOneCanvasFragment.args(session.id))
        }
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessions.collect { sessions ->
                adapter.submitList(sessions)
                binding.tvEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fabNew.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_meta)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class SessionAdapter(
    private val onClick: (OneOnOneSession) -> Unit
) : RecyclerView.Adapter<SessionAdapter.VH>() {

    private var items = listOf<OneOnOneSession>()

    fun submitList(list: List<OneOnOneSession>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.setOnClickListener { onClick(items[adapterPosition]) } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.binding.tvMember.text = s.memberName.ifBlank { "—" }
        holder.binding.tvDatum.text = s.datum.toGermanDate()
        holder.binding.tvTitel.text = s.titel
    }

    override fun getItemCount() = items.size
}
