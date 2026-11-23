package com.voicenotes.app.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotes.app.audio.AudioRecorder
import com.voicenotes.app.audio.RecordedClip
import com.voicenotes.app.data.ApiException
import com.voicenotes.app.data.AudioDto
import com.voicenotes.app.data.NotesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Форма создания и редактирования заметки (noteId == null: создание). Здесь же запись звука. */
class NoteFormViewModel(
    private val repo: NotesRepository,
    context: Context,
    noteId: String?,
) : ViewModel() {
    var title by mutableStateOf("")
    var text by mutableStateOf("")
    var existingAudio by mutableStateOf<List<AudioDto>>(emptyList())
        private set
    var clip by mutableStateOf<RecordedClip?>(null)
        private set
    var isRecording by mutableStateOf(false)
        private set
    var recordSeconds by mutableStateOf(0)
        private set
    var loading by mutableStateOf(noteId != null)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    // Если заметка уже создана, а загрузка записи не удалась, повторное сохранение только обновит её
    private var savedId: String? = noteId
    private val recorder = AudioRecorder(context.applicationContext)
    private var timerJob: Job? = null

    init {
        if (noteId != null) {
            viewModelScope.launch {
                try {
                    val note = repo.note(noteId)
                    title = note.title
                    text = note.text.orEmpty()
                    existingAudio = note.audioRecords
                } catch (e: ApiException) {
                    error = e.message
                } finally {
                    loading = false
                }
            }
        }
    }

    // ---------- Запись звука ----------

    fun startRecording() {
        try {
            discardClip()
            recorder.start()
            isRecording = true
            recordSeconds = 0
            val startedAt = SystemClock.elapsedRealtime()
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(250)
                    recordSeconds = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
                    if (recordSeconds >= MAX_SECONDS) {
                        stopRecording() // лимит проекта: одна запись не длиннее 10 минут
                        return@launch
                    }
                }
            }
        } catch (e: Exception) {
            isRecording = false
            error = "Не удалось начать запись"
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        isRecording = false
        clip = recorder.stop()
        if (clip == null) error = "Запись слишком короткая"
    }

    fun discardClip() {
        clip?.file?.delete()
        clip = null
    }

    // ---------- Сохранение ----------

    fun save(onDone: () -> Unit) {
        val cleanTitle = title.trim()
        when {
            cleanTitle.isEmpty() -> error = "Введите заголовок"
            cleanTitle.length > 120 -> error = "Заголовок не длиннее 120 символов"
            // Правило предметной области: заметка содержит текст или хотя бы одну запись
            text.isBlank() && clip == null && existingAudio.isEmpty() -> error = "Добавьте текст или запись"
            else -> viewModelScope.launch {
                saving = true
                error = null
                try {
                    val id = repo.saveNote(savedId, cleanTitle, text).id
                    savedId = id
                    clip?.let {
                        repo.uploadClip(id, it)
                        clip = null
                    }
                    onDone()
                } catch (e: ApiException) {
                    error = e.message
                } finally {
                    saving = false
                }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        if (isRecording) recorder.cancel()
        clip?.file?.delete()
    }

    private companion object {
        const val MAX_SECONDS = 599
    }
}
