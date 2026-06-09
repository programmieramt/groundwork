package com.groundwork.programmieramt.ui.teamnote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.groundwork.programmieramt.databinding.FragmentTeamNoteBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeamNoteFragment : Fragment() {

    private var _binding: FragmentTeamNoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeamNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
