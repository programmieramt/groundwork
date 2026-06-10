package com.groundwork.programmieramt.ui.oneonone

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentOneOnOneDetailBinding
import com.groundwork.programmieramt.db.entity.OneOnOneSessionEntity
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import com.groundwork.programmieramt.pen.FormTemplate
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class OneOnOneDetailFragment : Fragment() {

    private var _binding: FragmentOneOnOneDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OneOnOneViewModel by viewModels()

    private var existingId: Long = 0L
    private var datumMs: Long = System.currentTimeMillis()
    private var selectedMember: TeamMemberEntity? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOneOnOneDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.drawingSurface.drawTemplate = { canvas, w, h -> FormTemplate.drawOneOnOne(canvas, w, h) }

        binding.penToolbar.onToolSelected = { color, strokeWidth, isMarker ->
            binding.drawingSurface.setTool(color, strokeWidth, isMarker)
        }
        binding.penToolbar.currentTool().let { (color, strokeWidth, isMarker) ->
            binding.drawingSurface.setTool(color, strokeWidth, isMarker)
        }

        binding.etDatum.setText(datumMs.toGermanDate())
        binding.etDatum.setOnClickListener { showDatePicker() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.members.collect { members -> setupMemberDropdown(members) }
        }

        val sessionId = arguments?.getLong("session_id", 0L) ?: 0L
        if (sessionId > 0L) loadExisting(sessionId)

        binding.btnSave.setOnClickListener { save() }
    }

    private fun setupMemberDropdown(members: List<TeamMemberEntity>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, members.map { it.name })
        binding.etMember.setAdapter(adapter)
        binding.etMember.setOnItemClickListener { _, _, position, _ ->
            selectedMember = members[position]
            binding.tilMember.error = null
        }
        selectedMember?.let { binding.etMember.setText(it.name, false) }
    }

    private fun loadExisting(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val session = viewModel.getSessionById(id) ?: return@launch
            existingId = session.id
            datumMs = session.datum
            binding.etDatum.setText(datumMs.toGermanDate())
            binding.drawingSurface.setStrokesJson(session.strokes)
            viewModel.members.value.find { it.id == session.teamMemberId }?.let {
                selectedMember = it
                binding.etMember.setText(it.name, false)
            }
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
        val nameText = binding.etMember.text?.toString()?.trim() ?: ""
        if (nameText.isBlank()) {
            binding.tilMember.error = getString(R.string.error_member_required)
            return
        }
        binding.tilMember.error = null

        viewLifecycleOwner.lifecycleScope.launch {
            val member = selectedMember
                ?: viewModel.members.value.find { it.name.equals(nameText, ignoreCase = true) }
                ?: run {
                    val id = viewModel.insertMember(TeamMemberEntity(name = nameText))
                    TeamMemberEntity(id = id, name = nameText)
                }

            val sessionNr = if (existingId == 0L) viewModel.countByMember(member.id) + 1 else {
                viewModel.getSessionById(existingId)?.sessionNumber ?: 1
            }
            viewModel.save(OneOnOneSessionEntity(
                id = existingId,
                teamMemberId = member.id,
                datum = datumMs,
                sessionNumber = sessionNr,
                strokes = binding.drawingSurface.getStrokesJson(),
                updatedAt = System.currentTimeMillis()
            ))
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
