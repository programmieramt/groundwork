package com.groundwork.programmieramt.ui.voice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentJournalSearchBinding
import com.groundwork.programmieramt.fi.JournalClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class JournalSearchFragment : Fragment() {

    @Inject lateinit var journalClient: JournalClient

    private var _binding: FragmentJournalSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJournalSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSearch.setOnClickListener { performSearch() }

        binding.etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
    }

    private fun performSearch() {
        val query = binding.etQuery.text?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), R.string.search_empty_query, Toast.LENGTH_SHORT).show()
            return
        }

        binding.tvResults.text = getString(R.string.search_searching)
        binding.btnSearch.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { journalClient.search(query) }
            binding.btnSearch.isEnabled = true

            result.fold(
                onSuccess = { entries ->
                    if (entries.isEmpty()) {
                        binding.tvResults.text = getString(R.string.search_no_results)
                    } else {
                        binding.tvResults.text = entries.joinToString("\n\n") { entry ->
                            val dateMatch = Regex("""^\[(\d{4}-\d{2}-\d{2})]\s*""").find(entry)
                            if (dateMatch != null) {
                                val date = dateMatch.groupValues[1]
                                val text = entry.removePrefix(dateMatch.value)
                                "[$date]\n$text"
                            } else {
                                entry
                            }
                        }
                    }
                },
                onFailure = { e ->
                    binding.tvResults.text = getString(R.string.search_error, e.message ?: "")
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
