package com.voicenotes.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/** Готовая запись: файл M4A и её длительность в секундах. */
class RecordedClip(val file: File, val seconds: Double)

/** Запись звука с микрофона в формате AAC в контейнере M4A (как требует проект). */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    fun start() {
        val target = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(64_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(target.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = target
        startedAt = SystemClock.elapsedRealtime()
    }

    /** Останавливает запись. Возвращает null, если запись не удалась (например, слишком короткая). */
    fun stop(): RecordedClip? {
        val r = recorder ?: return null
        val f = file ?: return null
        recorder = null
        file = null
        val seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000.0
        return try {
            r.stop()
            r.release()
            RecordedClip(f, seconds.coerceAtLeast(0.1))
        } catch (e: RuntimeException) {
            r.release()
            f.delete()
            null
        }
    }

    fun cancel() {
        stop()?.file?.delete()
    }
}
