package com.voicenotes.app.audio

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/** Обёртка над ExoPlayer: одновременно играет одна запись, playingId говорит, какая именно. */
class AudioPlayerController(context: Context) {
    private val player = ExoPlayer.Builder(context).build()

    var playingId by mutableStateOf<String?>(null)
        private set

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) playingId = null
            }
        })
    }

    /** Нажатие на кнопку: если эта запись играет, ставим на паузу, иначе запускаем. */
    fun toggle(id: String, file: File) {
        if (playingId == id) {
            player.pause()
            playingId = null
            return
        }
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.play()
        playingId = id
    }

    fun release() {
        player.release()
    }
}
