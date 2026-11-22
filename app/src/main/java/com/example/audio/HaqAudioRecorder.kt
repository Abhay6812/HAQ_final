package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

private const val TAG = "HaqAudioRecorder"

/**
 * Lightweight, Zero-PII microphone recorder for Gemini Audio Transcription (`gemini-3.5-transcribe`).
 * Records AAC audio into app cache, returns the raw bytes on stop, and deletes the temp file immediately.
 */
class HaqAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    fun startRecording(): Result<Unit> {
        return try {
            stopAndDiscard()
            val file = File(context.cacheDir, "haq_farmer_voice_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioChannels(1)
                setAudioEncodingBitRate(32_000)
                setMaxDuration(90_000) // 90 seconds max (~360 KB, well under 20 MB limit)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            outputFile = file
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start microphone recording", e)
            stopAndDiscard()
            Result.failure(e)
        }
    }

    fun stopAndReadBytes(): Result<ByteArray> {
        val activeRecorder = recorder
        val file = outputFile
        recorder = null
        outputFile = null

        return try {
            if (activeRecorder == null || file == null) {
                return Result.failure(IllegalStateException("No active audio recording found."))
            }
            try {
                activeRecorder.stop()
            } finally {
                activeRecorder.reset()
                activeRecorder.release()
            }
            if (!file.exists() || file.length() <= 0L) {
                return Result.failure(IllegalStateException("Recorded audio was empty. Please speak clearly and try again."))
            }
            val bytes = file.readBytes()
            // Zero-PII cleanup: never retain farmer voice recordings on disk
            file.delete()
            Result.success(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop/read audio recording", e)
            file?.delete()
            Result.failure(e)
        }
    }

    fun stopAndDiscard() {
        val activeRecorder = recorder
        val file = outputFile
        recorder = null
        outputFile = null
        if (activeRecorder != null) {
            try {
                activeRecorder.stop()
            } catch (_: Exception) {
                // Ignore if stopped before audio frames arrived
            }
            try {
                activeRecorder.reset()
                activeRecorder.release()
            } catch (_: Exception) {
            }
        }
        file?.delete()
    }
}
