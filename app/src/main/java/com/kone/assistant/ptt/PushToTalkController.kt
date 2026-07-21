package com.kone.assistant.ptt

import android.content.Context
import com.kone.assistant.action.ActionResult
import com.kone.assistant.action.FakeActionExecutor
import com.kone.assistant.audio.AudioCapture
import com.kone.assistant.audio.CaptureProgress
import com.kone.assistant.audio.vad.RmsVoiceActivityDetector
import com.kone.assistant.audio.vad.RmsVadConfig
import com.kone.assistant.audio.vad.SpeechSegmenter
import com.kone.assistant.command.CommandResolver
import com.kone.assistant.command.Intent
import com.kone.assistant.stt.SttEvent
import com.kone.assistant.stt.VoskTranscriber
import java.io.Closeable
import java.io.File
import java.time.Instant

data class PipelineResult(
    val transcript: String,
    val intent: Intent,
    val action: ActionResult,
)

class PushToTalkController(
    context: Context,
    private val onSttEvent: (SttEvent) -> Unit,
    private val onProgress: (CaptureProgress) -> Unit,
    private val onPipelineResult: (PipelineResult) -> Unit,
    private val onError: (String) -> Unit,
) : Closeable {
    private val appContext = context.applicationContext
    private val resolver = CommandResolver()
    private val actionExecutor = FakeActionExecutor()
    private val logFile = File(appContext.filesDir, "lifecycle/push_to_talk.log")
    private val transcriber = VoskTranscriber(appContext, ::handleSttEvent, ::logMetric).also { it.load() }
    private val segmenter = SpeechSegmenter(
        // PTT users speak immediately; the persistent-listening 1 s calibration would cut the first word.
        detector = RmsVoiceActivityDetector(RmsVadConfig(calibrationMs = 0)),
        onSegment = { },
        onSpeechStarted = transcriber::startSpeech,
        onSpeechFrame = transcriber::acceptSpeechFrame,
        onSpeechEnded = transcriber::finishSpeech,
    )
    private val capture = AudioCapture(
        outputDirectory = File(appContext.cacheDir, "ptt_audio"),
        onProgress = onProgress,
        onFinished = { result -> result.file.delete() },
        onError = onError,
        speechSegmenter = segmenter,
    )

    val isRecording: Boolean get() = capture.isRecording

    fun press() {
        if (capture.isRecording) return
        logMetric("ptt_press")
        capture.start("ptt_${System.currentTimeMillis()}.pcm")
        logMetric("ptt_capture_started recording=${capture.isRecording}")
    }

    fun release() {
        logMetric("ptt_release recording=${capture.isRecording}")
        if (capture.isRecording) capture.stop()
    }

    private fun handleSttEvent(event: SttEvent) {
        onSttEvent(event)
        if (event is SttEvent.Final) {
            val intent = resolver.resolve(event.text)
            val action = actionExecutor.execute(intent)
            logMetric("pipeline intent=${intent.id} confidence=${intent.confidence.score} reason=${intent.reason.code} action=${action.status}")
            onPipelineResult(PipelineResult(event.text, intent, action))
        }
    }

    private fun logMetric(message: String) {
        runCatching {
            logFile.parentFile?.mkdirs()
            logFile.appendText("${Instant.now()} $message\n")
        }
    }

    override fun close() {
        release()
        transcriber.close()
    }
}
