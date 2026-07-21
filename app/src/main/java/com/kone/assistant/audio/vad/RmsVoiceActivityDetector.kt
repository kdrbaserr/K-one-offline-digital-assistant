package com.kone.assistant.audio.vad

import kotlin.math.max
import kotlin.math.sqrt

data class RmsVadConfig(
    val sampleRate: Int = 16_000,
    val calibrationMs: Int = 1_000,
    val noiseMultiplier: Double = 2.5,
    val minimumThresholdRms: Double = 0.012,
    val thresholdOffsetRms: Double = 0.004,
    val noiseSmoothing: Double = 0.05,
)

class RmsVoiceActivityDetector(private val config: RmsVadConfig = RmsVadConfig()) : VoiceActivityDetector {
    override val requiredSampleRate: Int = config.sampleRate
    private val calibrationSamples = config.sampleRate.toLong() * config.calibrationMs / 1_000
    private var seenSamples = 0L
    private var calibrationSquareSum = 0.0
    private var noiseFloor = 0.0

    override fun reset() {
        seenSamples = 0
        calibrationSquareSum = 0.0
        noiseFloor = 0.0
    }

    override fun analyze(samples: ShortArray, offset: Int, length: Int): VadDecision {
        require(offset >= 0 && length >= 0 && offset + length <= samples.size)
        val rms = calculateRms(samples, offset, length)
        val calibrating = seenSamples < calibrationSamples
        if (calibrating) {
            calibrationSquareSum += rms * rms * length
            seenSamples += length
            noiseFloor = sqrt(calibrationSquareSum / seenSamples.coerceAtLeast(1))
        } else if (rms < threshold()) {
            noiseFloor += config.noiseSmoothing * (rms - noiseFloor)
        }
        val threshold = threshold()
        val score = if (threshold <= 0.0) 0f else (rms / threshold / 4.0).toFloat().coerceIn(0f, 1f)
        return VadDecision(!calibrating && rms >= threshold, score, rms, noiseFloor, threshold, calibrating)
    }

    private fun threshold(): Double = max(
        config.minimumThresholdRms,
        noiseFloor * config.noiseMultiplier + config.thresholdOffsetRms,
    )

    private fun calculateRms(samples: ShortArray, offset: Int, length: Int): Double {
        if (length == 0) return 0.0
        var sum = 0.0
        for (index in offset until offset + length) {
            val normalized = samples[index] / 32768.0
            sum += normalized * normalized
        }
        return sqrt(sum / length)
    }
}
