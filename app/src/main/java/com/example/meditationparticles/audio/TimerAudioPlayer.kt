package com.example.meditationparticles.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.timer.TimerBellSoundChoice
import com.example.meditationparticles.domain.timer.TimerSoundOption

class TimerAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var bellPlayer: MediaPlayer? = null
    private var currentSound: TimerSoundOption = TimerSoundOption.None

    fun sync(
        sound: TimerSoundOption,
        shouldPlay: Boolean,
    ) {
        if (!shouldPlay || sound == TimerSoundOption.None) {
            stopAmbient()
            return
        }

        if (sound == currentSound && mediaPlayer?.isPlaying == true) {
            return
        }

        stopAmbient()

        val player = when (sound) {
            TimerSoundOption.None -> return
            TimerSoundOption.Rain -> createLoopingAmbientPlayer(R.raw.timer_rain)
            TimerSoundOption.Waves -> createLoopingAmbientPlayer(R.raw.timer_waves)
            TimerSoundOption.Forest -> createLoopingAmbientPlayer(R.raw.timer_forest)
        } ?: return

        currentSound = sound
        player.start()
        mediaPlayer = player
    }

    fun playBell(
        choice: TimerBellSoundChoice,
        systemUri: String?,
    ) {
        stopBell()
        val player = createBellPlayer(choice, systemUri) ?: return
        player.setOnCompletionListener {
            it.release()
            if (bellPlayer == it) {
                bellPlayer = null
            }
        }
        bellPlayer = player
        player.start()
    }

    fun stop() {
        stopBell()
        stopAmbient()
    }

    fun release() = stop()

    private fun stopAmbient() {
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentSound = TimerSoundOption.None
    }

    private fun stopBell() {
        bellPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        bellPlayer = null
    }

    private fun createBellPlayer(
        choice: TimerBellSoundChoice,
        systemUri: String?,
    ): MediaPlayer? = try {
        when (choice) {
            TimerBellSoundChoice.Default -> {
                MediaPlayer.create(context, R.raw.timer_med_bell)?.apply {
                    setAudioAttributes(notificationAttributes())
                    setVolume(1.0f, 1.0f)
                }
            }
            TimerBellSoundChoice.SystemUri -> {
                val uri = systemUri?.let(Uri::parse) ?: return null
                MediaPlayer().apply {
                    setAudioAttributes(notificationAttributes())
                    setDataSource(context, uri)
                    prepare()
                    setVolume(1.0f, 1.0f)
                }
            }
        }
    } catch (_: Exception) {
        null
    }

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

    private fun notificationAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
}
