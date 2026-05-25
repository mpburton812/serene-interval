package com.example.meditationparticles.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

class ToolkitAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    fun start(): String? {
        stop()
        val dir = File(context.filesDir, "toolkit_logs").apply { mkdirs() }
        val file = File(dir, "log_${UUID.randomUUID()}.m4a")
        outputFile = file

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            file.absolutePath
        } catch (_: Exception) {
            file.delete()
            outputFile = null
            null
        }
    }

    fun stop(): String? {
        val file = outputFile
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            file?.takeIf { it.exists() && it.length() > 0 }?.absolutePath
        } catch (_: Exception) {
            file?.delete()
            null
        } finally {
            recorder = null
            outputFile = null
        }
    }
}

class ToolkitAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var playingPath: String? = null

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    val currentPath: String?
        get() = playingPath

    fun toggle(path: String) {
        if (playingPath == path && mediaPlayer?.isPlaying == true) {
            stop()
            return
        }
        play(path)
    }

    fun play(path: String) {
        stop()
        val player = MediaPlayer()
        try {
            player.setDataSource(path)
            player.prepare()
            player.setOnCompletionListener { stop() }
            player.start()
            mediaPlayer = player
            playingPath = path
        } catch (_: Exception) {
            player.release()
        }
    }

    fun stop() {
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        playingPath = null
    }

    fun release() = stop()
}
