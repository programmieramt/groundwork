package com.groundwork.programmieramt.ui.sofort

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentSofortBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SofortFragment : Fragment() {

    private var _binding: FragmentSofortBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SofortViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSofortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SofortAdapter(
            onClick = { note ->
                findNavController().navigate(
                    R.id.action_sofort_to_detail,
                    bundleOf("note_id" to note.id)
                )
            },
            onLongClick = { note ->
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_delete_title)
                    .setMessage(R.string.dialog_delete_message)
                    .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(note) }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.syncAll { binding.swipeRefresh.isRefreshing = false }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collect { adapter.submitList(it) }
        }

        binding.fabNew.setOnClickListener {
            findNavController().navigate(
                R.id.action_sofort_to_detail,
                bundleOf("note_id" to 0L)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
