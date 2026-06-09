package com.groundwork.programmieramt.ui.sofort

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentSofortDetailBinding
import com.groundwork.programmieramt.db.entity.SofortNoteEntity
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class SofortDetailFragment : Fragment() {

    private var _binding: FragmentSofortDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SofortViewModel by viewModels()

    private var existingId: Long = 0L
    private var datumMs: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSofortDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setSectionTitles()
        binding.etDatum.setText(datumMs.toGermanDate())
        binding.etDatum.setOnClickListener { showDatePicker() }

        val noteId = arguments?.getLong("note_id", 0L) ?: 0L
        if (noteId > 0L) loadExisting(noteId)

        binding.btnSave.setOnClickListener { save() }
    }

    private fun setSectionTitles() {
        binding.headerCapture.tvSectionTitle.text = getString(R.string.section_capture)
        binding.headerFollowup.tvSectionTitle.text = getString(R.string.section_followup)
    }

    private fun loadExisting(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val note = viewModel.getNoteById(id) ?: return@launch
            existingId = note.id
            datumMs = note.datum
            binding.etDatum.setText(datumMs.toGermanDate())
            binding.etKategorie.setText(note.kategorie)
            binding.penCapture.setStrokesJson(note.capture)
            binding.penFollowup.setStrokesJson(note.followUp)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = datumMs }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            datumMs = cal.timeInMillis
            binding.etDatum.setText(dateFormat.format(cal.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun save() {
        viewModel.save(SofortNoteEntity(
            id = existingId,
            datum = datumMs,
            kategorie = binding.etKategorie.text?.toString()?.trim() ?: "",
            capture = binding.penCapture.getStrokesJson(),
            followUp = binding.penFollowup.getStrokesJson(),
            updatedAt = System.currentTimeMillis()
        ))
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
