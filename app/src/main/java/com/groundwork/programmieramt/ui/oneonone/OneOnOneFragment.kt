package com.groundwork.programmieramt.ui.oneonone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.groundwork.programmieramt.databinding.FragmentOneOnOneBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OneOnOneFragment : Fragment() {

    private var _binding: FragmentOneOnOneBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOneOnOneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
