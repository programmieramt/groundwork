package com.groundwork.programmieramt.ui.teamnote

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
import com.groundwork.programmieramt.databinding.FragmentTeamNoteDetailBinding
import com.groundwork.programmieramt.db.entity.TeamNoteEntity
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class TeamNoteDetailFragment : Fragment() {

    private var _binding: FragmentTeamNoteDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TeamNoteViewModel by viewModels()

    private var existingId: Long = 0L
    private var datumMs: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeamNoteDetailBinding.inflate(inflater, container, false)
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
        binding.headerBeobachtungen.tvSectionTitle.text = getString(R.string.section_beobachtungen)
        binding.headerStimmung.tvSectionTitle.text = getString(R.string.section_stimmung)
        binding.headerSpannungen.tvSectionTitle.text = getString(R.string.section_spannungen)
        binding.headerMassnahmen.tvSectionTitle.text = getString(R.string.section_massnahmen)
    }

    private fun loadExisting(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val note = viewModel.getNoteById(id) ?: return@launch
            existingId = note.id
            datumMs = note.datum
            binding.etDatum.setText(datumMs.toGermanDate())
            binding.etKontext.setText(note.kontextMeeting)
            binding.penBeobachtungen.setStrokesJson(note.beobachtungen)
            binding.penStimmung.setStrokesJson(note.stimmungDynamik)
            binding.penSpannungen.setStrokesJson(note.spannungenOffenePunkte)
            binding.penMassnahmen.setStrokesJson(note.massnahmenFollowUp)
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
        viewModel.save(TeamNoteEntity(
            id = existingId,
            datum = datumMs,
            kontextMeeting = binding.etKontext.text?.toString()?.trim() ?: "",
            beobachtungen = binding.penBeobachtungen.getStrokesJson(),
            stimmungDynamik = binding.penStimmung.getStrokesJson(),
            spannungenOffenePunkte = binding.penSpannungen.getStrokesJson(),
            massnahmenFollowUp = binding.penMassnahmen.getStrokesJson(),
            updatedAt = System.currentTimeMillis()
        ))
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
