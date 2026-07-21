package com.kone.assistant.audio.vad

data class VadDecision(
    val isSpeech: Boolean,
    val score: Float,
    val rms: Double,
    val noiseFloorRms: Double,
    val thresholdRms: Double,
    val isCalibrating: Boolean,
)

/** Common boundary for local RMS, Silero and whisper.cpp VAD implementations. */
interface VoiceActivityDetector {
    val requiredSampleRate: Int
    fun reset()
    fun analyze(samples: ShortArray, offset: Int = 0, length: Int = samples.size): VadDecision
}
