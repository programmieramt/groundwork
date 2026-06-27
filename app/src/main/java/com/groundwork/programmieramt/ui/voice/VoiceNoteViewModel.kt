package com.groundwork.programmieramt.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.fi.JournalClient
import com.groundwork.programmieramt.fi.JournalResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class VoiceNoteState {
    object Idle : VoiceNoteState()
    object Transcribing : VoiceNoteState()
    data class Transcribed(val text: String) : VoiceNoteState()
    object Saving : VoiceNoteState()
    data class Saved(val response: JournalResponse) : VoiceNoteState()
    data class Error(val message: String) : VoiceNoteState()
}

@HiltViewModel
class VoiceNoteViewModel @Inject constructor(
    private val journalClient: JournalClient
) : ViewModel() {

    private val _state = MutableStateFlow<VoiceNoteState>(VoiceNoteState.Idle)
    val state: StateFlow<VoiceNoteState> = _state

    fun transcribe(audioBytes: ByteArray) {
        viewModelScope.launch {
            _state.value = VoiceNoteState.Transcribing
            val result = withContext(Dispatchers.IO) { journalClient.transcribe(audioBytes) }
            _state.value = result.fold(
                onSuccess = { VoiceNoteState.Transcribed(it) },
                onFailure = { VoiceNoteState.Error("Transkription fehlgeschlagen: ${it.message}") }
            )
        }
    }

    fun saveToJournal(text: String, date: String) {
        viewModelScope.launch {
            _state.value = VoiceNoteState.Saving
            val result = withContext(Dispatchers.IO) { journalClient.saveJournal(text, date) }
            _state.value = result.fold(
                onSuccess = { VoiceNoteState.Saved(it) },
                onFailure = { VoiceNoteState.Error("Speichern fehlgeschlagen: ${it.message}") }
            )
        }
    }

    fun resetState() {
        _state.value = VoiceNoteState.Idle
    }
}
