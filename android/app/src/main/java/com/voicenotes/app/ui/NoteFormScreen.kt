package com.voicenotes.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Форма создания и редактирования заметки: заголовок, текст и запись звука. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFormScreen(vm: NoteFormViewModel, isEdit: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current

    // Разрешение на микрофон запрашивается только перед первой записью
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording() else vm.error = "Без доступа к микрофону запись невозможна"
    }
    fun onRecordClick() {
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Изменить заметку" else "Новая заметка") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                },
            )
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Button(
                    onClick = { vm.save(onBack) },
                    enabled = !vm.saving && !vm.loading && !vm.isRecording,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (vm.saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Сохранить")
                }
            }
        },
    ) { padding ->
        if (vm.loading) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = vm.title,
                    onValueChange = { vm.title = it },
                    label = { Text("Заголовок") },
                    supportingText = { Text("${vm.title.length} / 120") },
                    isError = vm.title.length > 120,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = vm.text,
                    onValueChange = { vm.text = it },
                    label = { Text("Текст заметки") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))
                Text("Голосовая запись", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                RecordingBlock(vm, onRecord = ::onRecordClick)

                vm.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RecordingBlock(vm: NoteFormViewModel, onRecord: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            val clip = vm.clip
            when {
                vm.isRecording -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFD32F2F)))
                    Spacer(Modifier.width(10.dp))
                    Text("Идёт запись  ${formatDuration(vm.recordSeconds)}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Button(onClick = vm::stopRecording) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Стоп")
                    }
                }

                clip != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Новая запись", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${formatDuration(clip.seconds)} · будет прикреплена при сохранении",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = vm::discardClip) { Icon(Icons.Filled.Delete, contentDescription = "Удалить запись") }
                }

                else -> OutlinedButton(onClick = onRecord, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (vm.existingAudio.isEmpty()) "Записать голос" else "Записать ещё")
                }
            }
            if (vm.existingAudio.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Уже прикреплено записей: ${vm.existingAudio.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
