package com.example.meditationparticles.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.meditationparticles.domain.visualizations.SceneAmbientSound

class AmbientAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: SceneAmbientSound? = null

    fun sync(
        sound: SceneAmbientSound,
        shouldPlay: Boolean,
        volume: Float = 0.55f,
    ) {
        if (!shouldPlay) {
            stop()
            return
        }

        if (sound == currentSound && mediaPlayer?.isPlaying == true) {
            return
        }

        stop()

        val player = MediaPlayer.create(context, sound.resourceId) ?: return
        currentSound = sound
        player.isLooping = true
        player.setVolume(volume, volume)
        player.start()
        mediaPlayer = player
    }

    fun stop() {
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentSound = null
    }

    fun release() = stop()
}
