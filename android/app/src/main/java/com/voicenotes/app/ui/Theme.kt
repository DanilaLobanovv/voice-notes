package com.voicenotes.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5D8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F5),
    onPrimaryContainer = Color(0xFF0B2A47),
    background = Color(0xFFF6F8FB),
    surface = Color(0xFFF6F8FB),
    surfaceContainerLow = Color.White,
    surfaceContainerHighest = Color(0xFFE6EBF1),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC3EE),
    onPrimary = Color(0xFF0B2A47),
    primaryContainer = Color(0xFF1F4A75),
    onPrimaryContainer = Color(0xFFD6E4F5),
)

@Composable
fun VoiceNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

// ---------- Форматирование для экранов ----------

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("ru"))

/** Превращает дату сервера (ISO, UTC) в «23 нояб. 2026, 15:51» по времени телефона. */
fun formatDate(iso: String): String = try {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(dateFormat)
} catch (e: Exception) {
    iso
}

/** 65.4 -> «1:05» */
fun formatDuration(seconds: Double): String = formatDuration(seconds.roundToInt())

fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
