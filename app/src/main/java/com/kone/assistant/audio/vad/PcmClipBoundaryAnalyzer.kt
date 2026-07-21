package com.kone.assistant.audio.vad

import java.io.File

data class ClipBoundaryReport(
    val clip: String,
    val category: String,
    val durationMs: Long,
    val segments: List<SpeechSegment>,
) {
    fun toCsvRows(): List<String> = if (segments.isEmpty()) {
        listOf("$clip,$category,$durationMs,-1,-1,0,0.0,0.0")
    } else {
        segments.map { segment ->
            "$clip,$category,$durationMs,${segment.startedAtMs},${segment.endedAtMs},${segment.endedAtMs - segment.startedAtMs},${segment.noiseFloorRms},${segment.thresholdRms}"
        }
    }
}

/** Offline evaluator used to inspect the same VAD boundaries on captured 16-bit LE PCM clips. */
class PcmClipBoundaryAnalyzer(
    private val detectorFactory: () -> VoiceActivityDetector = { RmsVoiceActivityDetector() },
    private val config: SpeechSegmenterConfig = SpeechSegmenterConfig(),
) {
    fun analyze(file: File, category: String): ClipBoundaryReport {
        val bytes = file.readBytes()
        require(bytes.size % 2 == 0) { "PCM byte count must be even: ${file.name}" }
        val samples = ShortArray(bytes.size / 2) { index ->
            val low = bytes[index * 2].toInt() and 0xff
            val high = bytes[index * 2 + 1].toInt()
            ((high shl 8) or low).toShort()
        }
        val segments = mutableListOf<SpeechSegment>()
        val segmenter = SpeechSegmenter(detectorFactory(), config, segments::add)
        segmenter.accept(samples)
        segmenter.flush()
        return ClipBoundaryReport(file.name, category, samples.size * 1_000L / config.sampleRate, segments)
    }

    companion object {
        const val CSV_HEADER = "clip,category,clip_duration_ms,start_ms,end_ms,segment_duration_ms,noise_floor_rms,threshold_rms"
    }
}
