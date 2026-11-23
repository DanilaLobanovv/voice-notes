package com.voicenotes.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voicenotes.app.audio.AudioPlayerController

/** Просмотр заметки: заголовок, текст и проигрыватель для каждой записи. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(vm: NoteViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val player = remember { AudioPlayerController(context) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(Unit) { vm.load() }

    var confirmDeleteNote by remember { mutableStateOf(false) }
    var audioToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заметка") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Изменить") }
                    IconButton(onClick = { confirmDeleteNote = true }) { Icon(Icons.Filled.Delete, contentDescription = "Удалить") }
                },
            )
        },
    ) { padding ->
        val note = vm.note
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                note == null && vm.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                note == null -> Text(
                    vm.error ?: "Заметка не найдена",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(note.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Изменено: ${formatDate(note.updatedAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (note.text.isNullOrBlank()) "Текста нет" else note.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (note.text.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )

                    if (note.audioRecords.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Аудиозаписи", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            note.audioRecords.forEachIndexed { index, audio ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FilledIconButton(onClick = {
                                            vm.play(audio.id) { file -> player.toggle(audio.id, file) }
                                        }) {
                                            if (vm.busyAudioId == audio.id) {
                                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                            } else if (player.playingId == audio.id) {
                                                Icon(Icons.Filled.Pause, contentDescription = "Пауза")
                                            } else {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = "Воспроизвести")
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Запись ${index + 1}", style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${formatDuration(audio.durationSec)} · ${formatDate(audio.createdAt)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = { audioToDelete = audio.id }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Удалить запись")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    vm.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDeleteNote) {
        AlertDialog(
            onDismissRequest = { confirmDeleteNote = false },
            title = { Text("Удалить заметку?") },
            text = { Text("Заметка и все её записи будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { confirmDeleteNote = false; vm.deleteNote(onBack) }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteNote = false }) { Text("Отмена") } },
        )
    }
    audioToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { audioToDelete = null },
            title = { Text("Удалить запись?") },
            confirmButton = {
                TextButton(onClick = { audioToDelete = null; vm.deleteAudio(id) }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { audioToDelete = null }) { Text("Отмена") } },
        )
    }
}
