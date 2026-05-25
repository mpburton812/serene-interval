package com.example.meditationparticles.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.meditationparticles.domain.visualizations.SceneAmbientSound

class AmbientAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: SceneAmbientSound? = null

    fun sync(
        sound: SceneAmbientSound,
        shouldPlay: Boolean,
        volume: Float = 1.0f,
    ) {
        if (!shouldPlay) {
            stop()
            return
        }

        if (sound == currentSound && mediaPlayer?.isPlaying == true) {
            return
        }

        stop()

        val player = createLoopingAmbientPlayer(sound.resourceId) ?: return
        player.setVolume(volume, volume)
        currentSound = sound
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

    private fun createLoopingAmbientPlayer(resId: Int): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setDataSource(
                context,
                Uri.parse("android.resource://${context.packageName}/$resId"),
            )
            prepare()
            isLooping = true
        }
    } catch (_: Exception) {
        null
    }
}
