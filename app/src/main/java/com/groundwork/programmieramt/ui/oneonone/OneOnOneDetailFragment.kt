package com.groundwork.programmieramt.ui.oneonone

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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentOneOnOneDetailBinding
import com.groundwork.programmieramt.db.entity.OneOnOneSessionEntity
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import com.groundwork.programmieramt.pen.FormTemplate
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class OneOnOneDetailFragment : Fragment() {

    private var _binding: FragmentOneOnOneDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OneOnOneViewModel by viewModels()

    private var existingId: Long = 0L
    private var datumMs: Long = System.currentTimeMillis()
    private var titel: String = ""
    private var selectedMember: TeamMemberEntity? = null
    private var memberName: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOneOnOneDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.drawingSurface.drawTemplate = { canvas, w, h -> FormTemplate.drawOneOnOne(canvas, w, h) }
        binding.penToolbar.onToolSelected = { tool ->
            binding.drawingSurface.setTool(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)
        }
        binding.penToolbar.currentTool().let { tool ->
            binding.drawingSurface.setTool(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)
        }

        binding.btnEditMeta.setOnClickListener { showMetadataDialog() }
        binding.btnSave.setOnClickListener { save() }

        val sessionId = arguments?.getLong("session_id", 0L) ?: 0L
        if (sessionId > 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                loadExisting(sessionId)
                showMetadataDialog()
            }
        } else {
            showMetadataDialog()
        }
    }

    private suspend fun loadExisting(id: Long) {
        val session = viewModel.getSessionById(id) ?: return
        existingId = session.id
        datumMs = session.datum
        titel = session.titel
        selectedMember = viewModel.members.value.find { it.id == session.teamMemberId }
        memberName = selectedMember?.name ?: ""
        binding.drawingSurface.setStrokesJson(session.strokes)
        updateHeader()
    }

    private fun updateHeader() {
        val parts = buildList {
            if (memberName.isNotBlank()) add(memberName)
            add(datumMs.toGermanDate())
        }
        binding.tvMeta.text = parts.joinToString(" · ")
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

        // Member autocomplete
        val members = viewModel.members.value
        val etMember = AutoCompleteTextView(requireContext()).apply {
            hint = getString(R.string.field_name)
            setText(memberName)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            threshold = 1
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, members.map { it.name }))
            setOnItemClickListener { _, _, position, _ ->
                selectedMember = members[position]
                memberName = members[position].name
            }
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        container.addView(etMember)

        // Datum row
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
        container.addView(datumRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = p8 })

        val etTitel = EditText(requireContext()).apply {
            hint = getString(R.string.field_titel)
            setText(titel)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = p8 }
        }
        container.addView(etTitel)

        binding.drawingSurface.setRawDrawingPaused(true)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.label_one_on_one_detail))
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                memberName = etMember.text.toString().trim()
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
        if (memberName.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.error_member_required), Toast.LENGTH_SHORT).show()
            showMetadataDialog()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val member = selectedMember
                ?: viewModel.members.value.find { it.name.equals(memberName, ignoreCase = true) }
                ?: run {
                    val id = viewModel.insertMember(TeamMemberEntity(name = memberName))
                    TeamMemberEntity(id = id, name = memberName)
                }

            val sessionNr = if (existingId == 0L) viewModel.countByMember(member.id) + 1 else {
                viewModel.getSessionById(existingId)?.sessionNumber ?: 1
            }
            viewModel.save(OneOnOneSessionEntity(
                id = existingId,
                teamMemberId = member.id,
                datum = datumMs,
                titel = titel,
                sessionNumber = sessionNr,
                strokes = binding.drawingSurface.getStrokesJson(),
                updatedAt = System.currentTimeMillis()
            ))
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!binding.drawingSurface.rawDrawingPaused) binding.drawingSurface.resumeDrawing()
    }

    override fun onPause() {
        super.onPause()
        binding.drawingSurface.pauseDrawing()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
