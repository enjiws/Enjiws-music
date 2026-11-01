package com.enjiws.music.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.enjiws.music.model.SongModel

class MusicPlayerManager(private val context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun playSong(song: SongModel) {
        player.stop()
        player.clearMediaItems()
        val mediaItem = MediaItem.fromUri(song.audioUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun release() {
        player.release()
    }
}
