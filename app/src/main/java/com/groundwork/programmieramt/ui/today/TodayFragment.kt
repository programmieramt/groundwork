package com.groundwork.programmieramt.ui.today

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentTodayBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TodayViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TodayAdapter(onClick = { item -> navigateTo(item) })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.entries.collect {
                adapter.submitList(it)
                binding.emptyView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun navigateTo(item: TodayEntry) {
        when (item.type) {
            TodayEntryType.SOFORT -> findNavController().navigate(
                R.id.action_today_to_sofort_detail, bundleOf("note_id" to item.id)
            )
            TodayEntryType.TEAM_NOTE -> findNavController().navigate(
                R.id.action_today_to_team_note_detail, bundleOf("note_id" to item.id)
            )
            TodayEntryType.ONE_ON_ONE -> findNavController().navigate(
                R.id.action_today_to_one_on_one_detail, bundleOf("session_id" to item.id)
            )
            TodayEntryType.MEETING -> findNavController().navigate(
                R.id.action_today_to_meeting_detail, bundleOf("note_id" to item.id)
            )
            TodayEntryType.FREE_NOTE -> findNavController().navigate(
                R.id.action_today_to_free_note_detail, bundleOf("note_id" to item.id)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
