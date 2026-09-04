package com.gps.zazor.utils.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records a short spoken note to sit beside a photo.
 *
 * In the field a note is often impossible to type - gloves, rain, one hand on a rope - so the same
 * remark is spoken instead. AAC in an MP4 container plays back everywhere and stays small: a
 * minute costs about 500 KB.
 */
class VoiceNoteRecorder(private val context: Context) {

    companion object {

        const val EXTENSION = ".m4a"
        private const val DIR_NOTES = "voice"
        private const val SAMPLE_RATE = 44_100
        private const val BIT_RATE = 64_000

        /** Hard stop so a forgotten recording cannot fill the phone. */
        const val MAX_DURATION_MS = 3 * 60 * 1000
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean get() = recorder != null

    private val directory: File
        get() = (context.getExternalFilesDir(DIR_NOTES) ?: File(context.filesDir, DIR_NOTES))
            .also { if (!it.exists()) it.mkdirs() }

    /**
     * @param onMaxDurationReached invoked on the main thread when the cap is hit; the caller is
     *        expected to call [stop], which is what actually finalises the file.
     * @return true when recording started.
     */
    fun start(onMaxDurationReached: () -> Unit = {}): Boolean {
        if (isRecording) return false
        val file = File(directory, "note_" + System.currentTimeMillis() + EXTENSION)
        @Suppress("DEPRECATION")
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        return try {
            created.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioEncodingBitRate(BIT_RATE)
                setMaxDuration(MAX_DURATION_MS)
                setOutputFile(file.absolutePath)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        onMaxDurationReached()
                    }
                }
                prepare()
                start()
            }
            recorder = created
            outputFile = file
            true
        } catch (e: Exception) {
            // Permission refused, microphone busy, or no encoder. The field is still null here,
            // so the local instance has to be released explicitly - otherwise every failed attempt
            // leaks a recorder that keeps holding the microphone.
            created.releaseQuietly()
            file.delete()
            false
        }
    }

    /**
     * @return the finished file, or null when nothing usable was captured. A recording shorter
     *         than a moment produces an unplayable file, so it is discarded instead of stored.
     */
    fun stop(): File? {
        val current = recorder ?: return null
        val file = outputFile
        val stoppedCleanly = try {
            current.stop()
            true
        } catch (e: RuntimeException) {
            // stop() throws when it was started but nothing was recorded.
            false
        } finally {
            releaseQuietly()
        }
        return if (stoppedCleanly && file != null && file.exists() && file.length() > 0) {
            file
        } else {
            file?.delete()
            null
        }
    }

    /** Aborts and removes the partial file. */
    fun cancel() {
        val file = outputFile
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            // Nothing recorded yet; the file is deleted below either way.
        } finally {
            releaseQuietly()
            file?.delete()
        }
    }

    private fun releaseQuietly() {
        recorder?.releaseQuietly()
        recorder = null
        outputFile = null
    }

    private fun MediaRecorder.releaseQuietly() {
        try {
            reset()
            release()
        } catch (e: Exception) {
            // Already released.
        }
    }
}
