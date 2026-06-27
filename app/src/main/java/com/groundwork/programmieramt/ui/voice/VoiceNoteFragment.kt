package com.groundwork.programmieramt.ui.voice

import android.Manifest
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.databinding.FragmentVoiceNoteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class VoiceNoteFragment : Fragment() {

    private var _binding: FragmentVoiceNoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VoiceNoteViewModel by viewModels()

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else Toast.makeText(requireContext(), R.string.voice_permission_denied, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVoiceNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etDatum.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))

        binding.btnRecord.setOnClickListener {
            if (!isRecording) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    == PermissionChecker.PERMISSION_GRANTED
                ) {
                    startRecording()
                } else {
                    requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            } else {
                stopRecording()
            }
        }

        binding.btnSave.setOnClickListener {
            val text = binding.etTranscript.text?.toString()?.trim() ?: ""
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), R.string.voice_empty_text, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val context = binding.etKontext.text?.toString()?.trim() ?: ""
            val fullText = if (context.isNotEmpty()) "[$context] $text" else text
            val date = binding.etDatum.text?.toString()?.trim() ?: ""
            viewModel.saveToJournal(fullText, date)
        }

        binding.btnNew.setOnClickListener {
            binding.etTranscript.setText("")
            binding.etKontext.setText("")
            binding.resultsSection.visibility = View.GONE
            binding.btnSave.isEnabled = true
            viewModel.resetState()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is VoiceNoteState.Idle -> {
                            binding.tvStatus.text = getString(R.string.voice_status_ready)
                            binding.btnSave.isEnabled = binding.etTranscript.text?.isNotBlank() == true
                        }
                        is VoiceNoteState.Transcribing -> {
                            binding.tvStatus.text = getString(R.string.voice_status_transcribing)
                            binding.btnSave.isEnabled = false
                        }
                        is VoiceNoteState.Transcribed -> {
                            binding.tvStatus.text = getString(R.string.voice_status_transcribed)
                            val existing = binding.etTranscript.text?.toString()?.trim() ?: ""
                            binding.etTranscript.setText(
                                if (existing.isNotEmpty()) "$existing\n${state.text}" else state.text
                            )
                            binding.btnSave.isEnabled = true
                        }
                        is VoiceNoteState.Saving -> {
                            binding.tvStatus.text = getString(R.string.voice_status_saving)
                            binding.btnSave.isEnabled = false
                        }
                        is VoiceNoteState.Saved -> {
                            val r = state.response
                            binding.tvStatus.text = getString(R.string.voice_status_saved, r.storedCount)
                            binding.tvFacts.text = r.structuredFacts.joinToString("\n\n") { "• $it" }
                            binding.resultsSection.visibility = View.VISIBLE
                            binding.btnSave.isEnabled = false
                        }
                        is VoiceNoteState.Error -> {
                            binding.tvStatus.text = state.message
                            binding.btnSave.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun startRecording() {
        try {
            audioFile = File(requireContext().cacheDir, "voice_note.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(requireContext())
            } else {
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            binding.btnRecord.text = getString(R.string.voice_record_stop)
            binding.tvStatus.text = getString(R.string.voice_status_recording)
        } catch (e: Exception) {
            binding.tvStatus.text = getString(R.string.voice_error_recording, e.message ?: "")
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            binding.btnRecord.text = getString(R.string.voice_record_start)

            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    viewModel.transcribe(file.readBytes())
                } else {
                    binding.tvStatus.text = getString(R.string.voice_error_empty)
                }
            }
        } catch (e: Exception) {
            binding.tvStatus.text = getString(R.string.voice_error_recording, e.message ?: "")
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            binding.btnRecord.text = getString(R.string.voice_record_start)
        }
    }

    override fun onDestroyView() {
        if (isRecording) {
            try {
                mediaRecorder?.apply { stop(); release() }
            } catch (_: Exception) {}
            mediaRecorder = null
        }
        super.onDestroyView()
        _binding = null
    }
}
