package com.groundwork.programmieramt.ui.oneonone

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentOneOnOneMetaBinding
import com.groundwork.programmieramt.db.entity.OneOnOneSession
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

// Pure form fragment — no SurfaceView anywhere, zero EPD conflict during text input
@AndroidEntryPoint
class OneOnOneMetaFragment : Fragment() {

    private var _binding: FragmentOneOnOneMetaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OneOnOneViewModel by viewModels()

    private var sessionId = 0L
    private var datumMs = System.currentTimeMillis()
    private var existingStrokes = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOneOnOneMetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionId = arguments?.getLong(ARG_SESSION_ID, 0L) ?: 0L
        binding.tvDatum.text = datumMs.toGermanDate()

        if (sessionId > 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getById(sessionId)?.let { s ->
                    existingStrokes = s.strokes
                    datumMs = s.datum
                    binding.etMemberName.setText(s.memberName)
                    binding.etTitel.setText(s.titel)
                    binding.tvDatum.text = datumMs.toGermanDate()
                }
            }
        }

        binding.btnDatum.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = datumMs }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                datumMs = cal.timeInMillis
                binding.tvDatum.text = datumMs.toGermanDate()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnWeiter.setOnClickListener { saveAndProceed() }
    }

    private fun saveAndProceed() {
        val name = binding.etMemberName.text.toString().trim()
        val titel = binding.etTitel.text.toString().trim()

        viewLifecycleOwner.lifecycleScope.launch {
            val id = viewModel.save(
                OneOnOneSession(
                    id = sessionId,
                    memberName = name,
                    datum = datumMs,
                    titel = titel,
                    strokes = existingStrokes,
                    updatedAt = System.currentTimeMillis()
                )
            )
            val finalId = if (sessionId == 0L) id else sessionId

            if (sessionId == 0L) {
                // New session: go to canvas, remove this meta screen from back stack
                findNavController().navigate(
                    R.id.action_meta_to_canvas,
                    OneOnOneCanvasFragment.args(finalId),
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_one_on_one_list, false)
                        .build()
                )
            } else {
                // Editing existing: just go back to canvas
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SESSION_ID = "session_id"
        fun args(sessionId: Long = 0L) = bundleOf(ARG_SESSION_ID to sessionId)
    }
}
