package com.example.meditationparticles.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.timer.TimerSoundOption

class TimerAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: TimerSoundOption = TimerSoundOption.None

    fun sync(
        sound: TimerSoundOption,
        customUri: String?,
        shouldPlay: Boolean,
    ) {
        if (!shouldPlay || sound == TimerSoundOption.None) {
            stop()
            return
        }

        if (sound == currentSound && mediaPlayer?.isPlaying == true) {
            return
        }

        stop()

        val player = when (sound) {
            TimerSoundOption.None -> return
            TimerSoundOption.Custom -> {
                val uri = customUri?.let { Uri.parse(it) } ?: return
                MediaPlayer.create(context, uri)
            }
            TimerSoundOption.Rain -> MediaPlayer.create(context, R.raw.timer_rain)
            TimerSoundOption.Ocean -> MediaPlayer.create(context, R.raw.timer_ocean)
            TimerSoundOption.Forest -> MediaPlayer.create(context, R.raw.timer_forest)
            TimerSoundOption.Wind -> MediaPlayer.create(context, R.raw.timer_wind)
            TimerSoundOption.Bell -> MediaPlayer.create(context, R.raw.timer_bell)
        } ?: return

        currentSound = sound
        player.isLooping = true
        player.setVolume(0.55f, 0.55f)
        player.start()
        mediaPlayer = player
    }

    fun playCompletionChime() {
        stop()
        val player = MediaPlayer.create(context, R.raw.timer_complete) ?: return
        player.setVolume(0.7f, 0.7f)
        player.setOnCompletionListener { it.release() }
        player.start()
    }

    fun stop() {
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentSound = TimerSoundOption.None
    }

    fun release() = stop()
}
