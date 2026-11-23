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

/** Список заметок: загрузка с сервера и выход из аккаунта. */
class NotesViewModel(private val repo: NotesRepository) : ViewModel() {
    var notes by mutableStateOf<List<NoteDto>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                notes = repo.notes()
            } catch (e: ApiException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun logout() = repo.logout()
}
