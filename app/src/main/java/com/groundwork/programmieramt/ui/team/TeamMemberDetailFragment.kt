package com.groundwork.programmieramt.ui.team

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
import com.groundwork.programmieramt.databinding.FragmentTeamMemberDetailBinding
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
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
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeamMemberDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setSectionTitles()

        val memberId = arguments?.getLong(ARG_MEMBER_ID, 0L) ?: 0L
        if (memberId > 0L) {
            loadExisting(memberId)
        }

        binding.etErstkontakt.setOnClickListener { showDatePicker() }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun setSectionTitles() {
        binding.headerEindruck.tvSectionTitle.text = getString(R.string.section_erster_eindruck)
        binding.headerStaerken.tvSectionTitle.text = getString(R.string.section_staerken)
        binding.headerEntwicklung.tvSectionTitle.text = getString(R.string.section_entwicklungsfeld)
        binding.headerMotivation.tvSectionTitle.text = getString(R.string.section_motivation)
        binding.headerSensibles.tvSectionTitle.text = getString(R.string.section_sensibles)
    }

    private fun loadExisting(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val member = viewModel.getMemberById(id) ?: return@launch
            existingId = member.id
            erstkontaktMs = member.erstkontakt
            binding.etName.setText(member.name)
            binding.etRolle.setText(member.rolle)
            if (member.erstkontakt > 0L) {
                binding.etErstkontakt.setText(dateFormat.format(Date(member.erstkontakt)))
            }
            binding.etErsterEindruck.setText(member.ersterEindruck)
            binding.etStaerken.setText(member.staerken)
            binding.etEntwicklungsfeld.setText(member.entwicklungsfeld)
            binding.etMotivation.setText(member.motivation)
            binding.etSensibles.setText(member.sensibles)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        if (erstkontaktMs > 0L) cal.timeInMillis = erstkontaktMs
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            erstkontaktMs = cal.timeInMillis
            binding.etErstkontakt.setText(dateFormat.format(cal.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun save() {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        if (name.isBlank()) {
            binding.tilName.error = "Name erforderlich"
            return
        }
        binding.tilName.error = null

        val entity = TeamMemberEntity(
            id = existingId,
            name = name,
            rolle = binding.etRolle.text?.toString()?.trim() ?: "",
            erstkontakt = erstkontaktMs,
            ersterEindruck = binding.etErsterEindruck.text?.toString()?.trim() ?: "",
            staerken = binding.etStaerken.text?.toString()?.trim() ?: "",
            entwicklungsfeld = binding.etEntwicklungsfeld.text?.toString()?.trim() ?: "",
            motivation = binding.etMotivation.text?.toString()?.trim() ?: "",
            sensibles = binding.etSensibles.text?.toString()?.trim() ?: "",
            updatedAt = System.currentTimeMillis()
        )
        viewModel.save(entity)
        findNavController().popBackStack()
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
