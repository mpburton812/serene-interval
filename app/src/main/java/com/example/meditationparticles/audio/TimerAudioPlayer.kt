package com.example.meditationparticles.audio

import android.content.Context
import android.media.AudioAttributes
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
                createLoopingAmbientPlayer(uri)
            }
            TimerSoundOption.Rain -> createLoopingAmbientPlayer(R.raw.timer_rain)
            TimerSoundOption.Waves -> createLoopingAmbientPlayer(R.raw.timer_waves)
            TimerSoundOption.Forest -> createLoopingAmbientPlayer(R.raw.timer_forest)
            TimerSoundOption.Wind -> createLoopingAmbientPlayer(R.raw.timer_wind)
            TimerSoundOption.Bell -> createLoopingAmbientPlayer(R.raw.timer_bell)
        } ?: return

        currentSound = sound
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

    private fun createLoopingAmbientPlayer(resId: Int): MediaPlayer? =
        createLoopingAmbientPlayer(
            Uri.parse("android.resource://${context.packageName}/$resId"),
        )

    private fun createLoopingAmbientPlayer(uri: Uri): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setDataSource(context, uri)
            prepare()
            isLooping = true
            setVolume(1.0f, 1.0f)
        }
    } catch (_: Exception) {
        null
    }
}
