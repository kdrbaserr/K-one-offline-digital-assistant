package com.kone.assistant.stt

import android.content.Context
import android.os.Debug
import android.os.SystemClock
import java.io.Closeable
import java.util.concurrent.Executors
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

sealed interface SttEvent {
    data class ModelLoading(val message: String) : SttEvent
    data class ModelReady(val loadMs: Long, val pssDeltaKb: Long) : SttEvent
    data class Partial(val text: String) : SttEvent
    data class Final(val text: String, val latencyMs: Long) : SttEvent
    data class Error(val message: String) : SttEvent
}

class VoskTranscriber(
    context: Context,
    private val onEvent: (SttEvent) -> Unit,
    private val onMetric: (String) -> Unit,
) : Closeable {
    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "vosk-stt") }
    private var model: Model? = null
    private var streamRecognizer: Recognizer? = null
    private val committedText = mutableListOf<String>()

    fun load() {
        executor.execute {
            if (model != null) return@execute
            try {
                onEvent(SttEvent.ModelLoading("Türkçe model hazırlanıyor"))
                val started = SystemClock.elapsedRealtime()
                val pssBefore = Debug.getPss().toLong()
                val path = VoskModelAssetInstaller(appContext).install()
                model = Model(path.absolutePath)
                val loadMs = SystemClock.elapsedRealtime() - started
                val pssAfter = Debug.getPss().toLong()
                val heapKb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_024
                onMetric("vosk_model_loaded version=0.3 loadMs=$loadMs pssBeforeKb=$pssBefore pssAfterKb=$pssAfter pssDeltaKb=${pssAfter - pssBefore} heapUsedKb=$heapKb")
                onEvent(SttEvent.ModelReady(loadMs, pssAfter - pssBefore))
            } catch (error: Exception) {
                onMetric("vosk_model_error type=${error.javaClass.simpleName} message=${error.message}")
                onEvent(SttEvent.Error(error.message ?: "Model yüklenemedi"))
            }
        }
    }

    fun startSpeech(initialPcm: ShortArray) {
        executor.execute {
            val loadedModel = model
            if (loadedModel == null) {
                onEvent(SttEvent.Error("Türkçe model henüz hazır değil"))
                return@execute
            }
            try {
                streamRecognizer?.close()
                streamRecognizer = Recognizer(loadedModel, 16_000f)
                committedText.clear()
                acceptPcm(initialPcm)
            } catch (error: Exception) {
                onMetric("vosk_recognition_error type=${error.javaClass.simpleName} message=${error.message}")
                onEvent(SttEvent.Error(error.message ?: "Konuşma çözümlenemedi"))
            }
        }
    }

    fun acceptSpeechFrame(pcm: ShortArray) {
        executor.execute {
            try {
                acceptPcm(pcm)
            } catch (error: Exception) {
                onMetric("vosk_recognition_error type=${error.javaClass.simpleName} message=${error.message}")
                onEvent(SttEvent.Error(error.message ?: "Konuşma çözümlenemedi"))
            }
        }
    }

    fun finishSpeech() {
        val speechEndedAt = SystemClock.elapsedRealtime()
        executor.execute {
            val recognizer = streamRecognizer ?: return@execute
            try {
                val tail = JSONObject(recognizer.finalResult).optString("text").trim()
                val text = (committedText + tail).filter { it.isNotBlank() }.joinToString(" ")
                val latencyMs = SystemClock.elapsedRealtime() - speechEndedAt
                onMetric("vosk_final latencyMs=$latencyMs chars=${text.length}")
                onEvent(SttEvent.Final(text, latencyMs))
            } finally {
                recognizer.close()
                streamRecognizer = null
                committedText.clear()
            }
        }
    }

    private fun acceptPcm(pcm: ShortArray) {
        val recognizer = streamRecognizer ?: return
        val bytes = pcm.toLittleEndianBytes(0, pcm.size)
        if (recognizer.acceptWaveForm(bytes, bytes.size)) {
            val text = JSONObject(recognizer.result).optString("text").trim()
            if (text.isNotEmpty()) committedText.add(text)
        }
        val partial = JSONObject(recognizer.partialResult).optString("partial").trim()
        val combined = (committedText + partial).filter { it.isNotBlank() }.joinToString(" ")
        if (combined.isNotEmpty()) onEvent(SttEvent.Partial(combined))
    }

    private fun ShortArray.toLittleEndianBytes(offset: Int, length: Int): ByteArray = ByteArray(length * 2).also { bytes ->
        for (index in 0 until length) {
            val value = this[offset + index].toInt()
            bytes[index * 2] = (value and 0xff).toByte()
            bytes[index * 2 + 1] = ((value ushr 8) and 0xff).toByte()
        }
    }

    override fun close() {
        executor.execute { streamRecognizer?.close(); streamRecognizer = null; model?.close(); model = null }
        executor.shutdown()
    }
}
