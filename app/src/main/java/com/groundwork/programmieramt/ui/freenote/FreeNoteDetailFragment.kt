package com.groundwork.programmieramt.ui.freenote

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentFreeNoteDetailBinding
import com.groundwork.programmieramt.db.entity.FreeNoteEntity
import com.groundwork.programmieramt.pen.FormTemplate
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class FreeNoteDetailFragment : Fragment() {

    private var _binding: FragmentFreeNoteDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FreeNoteViewModel by viewModels()

    private var existingId: Long = 0L
    private var datumMs: Long = System.currentTimeMillis()
    private var titel: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFreeNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.drawingSurface.drawTemplate = { canvas, w, h -> FormTemplate.drawFreeNote(canvas, w, h) }
        binding.penToolbar.onToolSelected = { tool ->
            binding.drawingSurface.setTool(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)
        }
        binding.penToolbar.currentTool().let { tool ->
            binding.drawingSurface.setTool(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)
        }

        binding.btnEditMeta.setOnClickListener { showMetadataDialog() }
        binding.btnSave.setOnClickListener { save() }

        val noteId = arguments?.getLong("note_id", 0L) ?: 0L
        if (noteId > 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                loadExisting(noteId)
                showMetadataDialog()
            }
        } else {
            showMetadataDialog()
        }
    }

    private suspend fun loadExisting(id: Long) {
        val note = viewModel.getNoteById(id) ?: return
        existingId = note.id
        datumMs = note.datum
        titel = note.titel
        binding.drawingSurface.setStrokesJson(note.strokes)
        updateHeader()
    }

    private fun updateHeader() {
        binding.tvMeta.text = datumMs.toGermanDate()
        if (titel.isNotBlank()) {
            binding.tvTitel.visibility = View.VISIBLE
            binding.tvTitel.text = titel
        } else {
            binding.tvTitel.visibility = View.GONE
        }
    }

    private fun showMetadataDialog() {
        val dp = resources.displayMetrics.density
        val p8 = (8 * dp).toInt()
        val p16 = (16 * dp).toInt()

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(p16, p8, p16, p16)
        }

        val tvDatum = TextView(requireContext()).apply {
            text = datumMs.toGermanDate()
            textSize = 16f
        }
        val btnDatum = MaterialButton(requireContext(), null,
            com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = getString(R.string.action_change_date)
        }
        val datumRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(tvDatum, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(btnDatum)
        }
        container.addView(datumRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val etTitel = EditText(requireContext()).apply {
            hint = getString(R.string.field_titel)
            setText(titel)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = p8 }
        }
        container.addView(etTitel)

        binding.drawingSurface.setRawDrawingPaused(true)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.label_free_note_detail))
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                titel = etTitel.text.toString().trim()
                updateHeader()
            }
            .setNegativeButton("Zurück") { _, _ ->
                if (existingId == 0L) findNavController().popBackStack()
            }
            .create()

        dialog.setOnDismissListener { binding.drawingSurface.setRawDrawingPaused(false) }

        btnDatum.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = datumMs }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                datumMs = cal.timeInMillis
                tvDatum.text = datumMs.toGermanDate()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialog.show()
    }

    private fun save() {
        viewModel.save(FreeNoteEntity(
            id = existingId,
            datum = datumMs,
            titel = titel,
            strokes = binding.drawingSurface.getStrokesJson(),
            updatedAt = System.currentTimeMillis()
        ))
        findNavController().popBackStack()
    }

    override fun onResume() {
        super.onResume()
        binding.drawingSurface.resumeDrawing()
    }

    override fun onPause() {
        super.onPause()
        binding.drawingSurface.setRawDrawingPaused(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
