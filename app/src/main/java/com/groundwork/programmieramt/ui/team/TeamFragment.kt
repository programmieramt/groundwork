package com.groundwork.programmieramt.ui.team

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentTeamBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TeamFragment : Fragment() {

    private var _binding: FragmentTeamBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TeamViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TeamMemberAdapter { member ->
            findNavController().navigate(
                R.id.action_team_to_detail,
                TeamMemberDetailFragment.args(member.id)
            )
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.members.collect { adapter.submitList(it) }
        }

        binding.fabNew.setOnClickListener {
            findNavController().navigate(
                R.id.action_team_to_detail,
                TeamMemberDetailFragment.args(null)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
