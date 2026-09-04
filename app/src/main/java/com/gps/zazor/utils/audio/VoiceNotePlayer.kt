package com.gps.zazor.utils.audio

import android.media.MediaPlayer
import java.io.File

/**
 * Plays back a voice note. One note at a time - starting another stops the previous one, so two
 * recordings can never talk over each other.
 */
class VoiceNotePlayer {

    private var player: MediaPlayer? = null

    /** Absolute path of whatever is playing right now, or null. */
    var playingPath: String? = null
        private set

    fun play(path: String, onFinished: () -> Unit = {}): Boolean {
        stop()
        val file = File(path)
        if (!file.exists()) return false
        return try {
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    stop()
                    onFinished()
                }
                prepare()
                start()
            }
            playingPath = path
            true
        } catch (e: Exception) {
            stop()
            false
        }
    }

    fun stop() {
        try {
            player?.reset()
            player?.release()
        } catch (e: Exception) {
            // Already released.
        }
        player = null
        playingPath = null
    }
}
