package com.voicenotes.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotes.app.data.ApiException
import com.voicenotes.app.data.NoteDto
import com.voicenotes.app.data.NotesRepository
import kotlinx.coroutines.launch
import java.io.File

/** Просмотр одной заметки: загрузка, удаление заметки и записей, скачивание звука для проигрывателя. */
class NoteViewModel(private val repo: NotesRepository, private val noteId: String) : ViewModel() {
    var note by mutableStateOf<NoteDto?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var busyAudioId by mutableStateOf<String?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                note = repo.note(noteId)
            } catch (e: ApiException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    /** Скачивает запись (если ещё не скачана) и отдаёт файл проигрывателю. */
    fun play(audioId: String, onFile: (File) -> Unit) {
        viewModelScope.launch {
            busyAudioId = audioId
            try {
                onFile(repo.audioFile(audioId))
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busyAudioId = null
            }
        }
    }

    fun deleteAudio(audioId: String) {
        viewModelScope.launch {
            try {
                repo.deleteAudio(audioId)
                note = repo.note(noteId)
            } catch (e: ApiException) {
                error = e.message
            }
        }
    }

    fun deleteNote(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.deleteNote(noteId)
                onDone()
            } catch (e: ApiException) {
                error = e.message
            }
        }
    }
}
