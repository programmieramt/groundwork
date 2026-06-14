package com.groundwork.programmieramt.ui.meeting

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.groundwork.programmieramt.databinding.FragmentMeetingNoteDetailBinding
import com.groundwork.programmieramt.db.entity.MeetingNoteEntity
import com.groundwork.programmieramt.pen.FormTemplate
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class MeetingNoteDetailFragment : Fragment() {

    private var _binding: FragmentMeetingNoteDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeetingNoteViewModel by viewModels()

    private var existingId: Long = 0L
    private var datumMs: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMeetingNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.drawingSurface.drawTemplate = { canvas, w, h -> FormTemplate.drawMeetingNote(canvas, w, h) }

        binding.penToolbar.onToolSelected = { tool ->
            binding.drawingSurface.setTool(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)
        }
        binding.penToolbar.currentTool().let { tool ->
            binding.drawingSurface.setTool(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)
        }

        binding.etDatum.setText(datumMs.toGermanDate())
        binding.etDatum.setOnClickListener { showDatePicker() }

        val noteId = arguments?.getLong("note_id", 0L) ?: 0L
        if (noteId > 0L) loadExisting(noteId)

        binding.btnSave.setOnClickListener { save() }
    }

    private fun loadExisting(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val note = viewModel.getNoteById(id) ?: return@launch
            existingId = note.id
            datumMs = note.datum
            binding.etDatum.setText(datumMs.toGermanDate())
            binding.etTeilnehmer.setText(note.teilnehmer)
            binding.drawingSurface.setStrokesJson(note.strokes)
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
        viewModel.save(MeetingNoteEntity(
            id = existingId,
            datum = datumMs,
            teilnehmer = binding.etTeilnehmer.text?.toString()?.trim() ?: "",
            strokes = binding.drawingSurface.getStrokesJson(),
            updatedAt = System.currentTimeMillis()
        ))
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
