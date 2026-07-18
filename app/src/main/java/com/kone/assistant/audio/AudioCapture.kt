package com.kone.assistant.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class CaptureProgress(
    val elapsedMs: Long,
    val level: Float,
    val bytesWritten: Long,
    val readErrors: Int,
)

data class CaptureResult(
    val file: File,
    val durationMs: Long,
    val bytesWritten: Long,
    val readErrors: Int,
    val rms: Double,
    val clippingPercent: Double,
    val isSilent: Boolean,
    val expectedBytes: Long,
) {
    val bufferCoveragePercent: Double =
        if (expectedBytes == 0L) 0.0 else bytesWritten * 100.0 / expectedBytes
}

class AudioCapture(
    private val outputDirectory: File,
    private val onProgress: (CaptureProgress) -> Unit,
    private val onFinished: (CaptureResult) -> Unit,
    private val onError: (String) -> Unit,
) {
    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNEL_COUNT = 1
        const val BYTES_PER_SAMPLE = 2
    }

    @Volatile
    private var recording = false
    private var worker: Thread? = null
    private var audioRecord: AudioRecord? = null

    val isRecording: Boolean get() = recording

    @SuppressLint("MissingPermission")
    fun start(fileName: String) {
        if (recording) return

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            onError("16 kHz mono PCM bu cihazda başlatılamadı: $minBuffer")
            return
        }

        val bufferSize = max(minBuffer * 2, 4096)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            onError("AudioRecord hazırlanamadı.")
            return
        }

        outputDirectory.mkdirs()
        val output = File(outputDirectory, fileName)
        audioRecord = recorder
        recording = true
        worker = Thread({ captureLoop(recorder, bufferSize, output) }, "pcm-capture").also { it.start() }
    }

    fun stop() {
        if (!recording) return
        recording = false
        audioRecord?.stop()
        worker?.join(2_000)
        worker = null
    }

    private fun captureLoop(recorder: AudioRecord, bufferSize: Int, output: File) {
        val buffer = ShortArray(bufferSize / BYTES_PER_SAMPLE)
        var totalSamples = 0L
        var sumSquares = 0.0
        var clippedSamples = 0L
        var bytesWritten = 0L
        var readErrors = 0
        val startedNs = System.nanoTime()

        try {
            FileOutputStream(output).use { stream ->
                recorder.startRecording()
                while (recording) {
                    val count = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (count > 0) {
                        val byteBuffer = ByteArray(count * BYTES_PER_SAMPLE)
                        var blockSquares = 0.0
                        for (index in 0 until count) {
                            val sample = buffer[index].toInt()
                            byteBuffer[index * 2] = (sample and 0xff).toByte()
                            byteBuffer[index * 2 + 1] = ((sample ushr 8) and 0xff).toByte()
                            val normalized = sample / 32768.0
                            blockSquares += normalized * normalized
                            if (abs(sample) >= 32760) clippedSamples++
                        }
                        stream.write(byteBuffer)
                        totalSamples += count
                        bytesWritten += byteBuffer.size
                        sumSquares += blockSquares
                        val elapsedMs = (System.nanoTime() - startedNs) / 1_000_000
                        onProgress(
                            CaptureProgress(
                                elapsedMs,
                                sqrt(blockSquares / count).toFloat().coerceIn(0f, 1f),
                                bytesWritten,
                                readErrors,
                            )
                        )
                    } else if (count != AudioRecord.ERROR_INVALID_OPERATION) {
                        readErrors++
                    }
                }
            }

            val durationMs = (System.nanoTime() - startedNs) / 1_000_000
            val rms = if (totalSamples == 0L) 0.0 else sqrt(sumSquares / totalSamples)
            val expectedBytes = durationMs * SAMPLE_RATE * BYTES_PER_SAMPLE / 1_000
            onFinished(
                CaptureResult(
                    file = output,
                    durationMs = durationMs,
                    bytesWritten = bytesWritten,
                    readErrors = readErrors,
                    rms = rms,
                    clippingPercent = if (totalSamples == 0L) 0.0 else clippedSamples * 100.0 / totalSamples,
                    isSilent = rms < 0.01,
                    expectedBytes = expectedBytes,
                )
            )
        } catch (error: Exception) {
            output.delete()
            onError("Kayıt hatası: ${error.message ?: error.javaClass.simpleName}")
        } finally {
            recording = false
            runCatching { recorder.stop() }
            recorder.release()
            audioRecord = null
        }
    }
}
