package com.groundwork.programmieramt.ui.team

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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentTeamMemberDetailBinding
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import com.groundwork.programmieramt.pen.FormTemplate
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class TeamMemberDetailFragment : Fragment() {

    private var _binding: FragmentTeamMemberDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TeamViewModel by viewModels()

    private var existingId: Long = 0L
    private var erstkontaktMs: Long = 0L
    private var name: String = ""
    private var rolle: String = ""
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeamMemberDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.drawingSurface.drawTemplate = { canvas, w, h -> FormTemplate.drawTeamMember(canvas, w, h) }
        binding.penToolbar.onToolSelected = { tool ->
            binding.drawingSurface.setTool(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)
        }
        binding.penToolbar.currentTool().let { tool ->
            binding.drawingSurface.setTool(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)
        }

        binding.btnEditMeta.setOnClickListener { showMetadataDialog() }
        binding.btnSave.setOnClickListener { save() }

        val memberId = arguments?.getLong(ARG_MEMBER_ID, 0L) ?: 0L
        if (memberId > 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                loadExisting(memberId)
                showMetadataDialog()
            }
        } else {
            showMetadataDialog()
        }
    }

    private suspend fun loadExisting(id: Long) {
        val member = viewModel.getMemberById(id) ?: return
        existingId = member.id
        erstkontaktMs = member.erstkontakt
        name = member.name
        rolle = member.rolle
        binding.drawingSurface.setStrokesJson(member.strokes)
        updateHeader()
    }

    private fun updateHeader() {
        val parts = buildList {
            if (rolle.isNotBlank()) add(rolle)
            if (erstkontaktMs > 0L) add(dateFormat.format(Date(erstkontaktMs)))
        }
        binding.tvMeta.text = parts.joinToString(" · ")
        if (name.isNotBlank()) {
            binding.tvTitel.visibility = View.VISIBLE
            binding.tvTitel.text = name
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

        val etName = EditText(requireContext()).apply {
            hint = getString(R.string.field_name)
            setText(name)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        container.addView(etName)

        val etRolle = EditText(requireContext()).apply {
            hint = getString(R.string.field_rolle_team)
            setText(rolle)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = p8 }
        }
        container.addView(etRolle)

        val tvErstkontakt = TextView(requireContext()).apply {
            text = if (erstkontaktMs > 0L) dateFormat.format(Date(erstkontaktMs)) else getString(R.string.field_erstkontakt)
            textSize = 16f
        }
        val btnErstkontakt = MaterialButton(requireContext(), null,
            com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = getString(R.string.action_change_date)
        }
        val datumRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(tvErstkontakt, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(btnErstkontakt)
        }
        container.addView(datumRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = p8 })

        binding.drawingSurface.setRawDrawingPaused(true)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.label_teammitglied))
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                name = etName.text.toString().trim()
                rolle = etRolle.text.toString().trim()
                updateHeader()
            }
            .setNegativeButton("Zurück") { _, _ ->
                if (existingId == 0L) findNavController().popBackStack()
            }
            .create()

        dialog.setOnDismissListener { binding.drawingSurface.setRawDrawingPaused(false) }

        btnErstkontakt.setOnClickListener {
            val cal = Calendar.getInstance()
            if (erstkontaktMs > 0L) cal.timeInMillis = erstkontaktMs
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                erstkontaktMs = cal.timeInMillis
                tvErstkontakt.text = dateFormat.format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialog.show()
    }

    private fun save() {
        if (name.isBlank()) {
            showMetadataDialog()
            return
        }
        viewModel.save(TeamMemberEntity(
            id = existingId,
            name = name,
            rolle = rolle,
            erstkontakt = erstkontaktMs,
            strokes = binding.drawingSurface.getStrokesJson(),
            updatedAt = System.currentTimeMillis()
        ))
        findNavController().popBackStack()
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

    companion object {
        private const val ARG_MEMBER_ID = "member_id"
        fun args(memberId: Long?) = bundleOf(ARG_MEMBER_ID to (memberId ?: 0L))
    }
}
