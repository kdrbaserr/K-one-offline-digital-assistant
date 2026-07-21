package com.kone.assistant.audio.vad

data class SpeechSegment(
    val pcm: ShortArray,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val noiseFloorRms: Double,
    val thresholdRms: Double,
)

data class SpeechSegmenterConfig(
    val sampleRate: Int = 16_000,
    val frameMs: Int = 20,
    val preRollMs: Int = 300,
    val postRollMs: Int = 500,
    val startFrames: Int = 2,
)

/** Converts continuous PCM into speech-only segments suitable for an STT engine. */
class SpeechSegmenter(
    private val detector: VoiceActivityDetector,
    private val config: SpeechSegmenterConfig = SpeechSegmenterConfig(),
    private val onSegment: (SpeechSegment) -> Unit,
    private val onSpeechStarted: (ShortArray) -> Unit = {},
    private val onSpeechFrame: (ShortArray) -> Unit = {},
    private val onSpeechEnded: () -> Unit = {},
) {
    private val frameSamples = config.sampleRate * config.frameMs / 1_000
    private val preRollFrames = (config.preRollMs / config.frameMs).coerceAtLeast(1)
    private val postRollFrames = (config.postRollMs / config.frameMs).coerceAtLeast(1)
    private val preRoll = ArrayDeque<ShortArray>()
    private val pendingFrame = ShortArray(frameSamples)
    private var pendingCount = 0
    private var processedSamples = 0L
    private var speechStartCandidateFrames = 0
    private var silenceFrames = 0
    private var activeFrames: MutableList<ShortArray>? = null
    private var activeStartSample = 0L
    private var lastDecision: VadDecision? = null

    init {
        require(detector.requiredSampleRate == config.sampleRate)
        require(frameSamples > 0 && config.startFrames > 0)
    }

    fun accept(samples: ShortArray, length: Int = samples.size) {
        require(length in 0..samples.size)
        var sourceOffset = 0
        while (sourceOffset < length) {
            val copied = minOf(frameSamples - pendingCount, length - sourceOffset)
            samples.copyInto(pendingFrame, pendingCount, sourceOffset, sourceOffset + copied)
            pendingCount += copied
            sourceOffset += copied
            if (pendingCount == frameSamples) {
                processFrame(pendingFrame.copyOf())
                pendingCount = 0
            }
        }
    }

    fun flush() {
        if (pendingCount > 0) {
            processFrame(pendingFrame.copyOf(pendingCount))
            pendingCount = 0
        }
        finishActiveSegment()
    }

    fun reset() {
        preRoll.clear()
        pendingCount = 0
        processedSamples = 0
        speechStartCandidateFrames = 0
        silenceFrames = 0
        activeFrames = null
        lastDecision = null
        detector.reset()
    }

    private fun processFrame(frame: ShortArray) {
        val decision = detector.analyze(frame)
        lastDecision = decision
        val active = activeFrames
        if (active == null) {
            addPreRoll(frame)
            speechStartCandidateFrames = if (decision.isSpeech) speechStartCandidateFrames + 1 else 0
            if (speechStartCandidateFrames >= config.startFrames) {
                activeStartSample = (processedSamples + frame.size - preRoll.sumOf { it.size }).coerceAtLeast(0)
                activeFrames = preRoll.mapTo(mutableListOf()) { it.copyOf() }
                onSpeechStarted(activeFrames!!.flattenToShortArray())
                preRoll.clear()
                silenceFrames = 0
            }
        } else {
            active.add(frame)
            onSpeechFrame(frame.copyOf())
            silenceFrames = if (decision.isSpeech) 0 else silenceFrames + 1
            if (silenceFrames >= postRollFrames) finishActiveSegment()
        }
        processedSamples += frame.size
    }

    private fun addPreRoll(frame: ShortArray) {
        preRoll.addLast(frame)
        // Keep the requested lead-in in addition to the frames used to confirm speech.
        while (preRoll.size > preRollFrames + config.startFrames) preRoll.removeFirst()
    }

    private fun finishActiveSegment() {
        val frames = activeFrames ?: return
        if (frames.isEmpty()) return
        val pcm = ShortArray(frames.sumOf { it.size })
        var offset = 0
        frames.forEach { frame -> frame.copyInto(pcm, offset); offset += frame.size }
        val endSample = activeStartSample + pcm.size
        val decision = lastDecision
        onSegment(SpeechSegment(pcm, activeStartSample * 1_000 / config.sampleRate,
            endSample * 1_000 / config.sampleRate, decision?.noiseFloorRms ?: 0.0,
            decision?.thresholdRms ?: 0.0))
        onSpeechEnded()
        activeFrames = null
        speechStartCandidateFrames = 0
        silenceFrames = 0
    }

    private fun List<ShortArray>.flattenToShortArray(): ShortArray {
        val result = ShortArray(sumOf { it.size })
        var offset = 0
        forEach { frame -> frame.copyInto(result, offset); offset += frame.size }
        return result
    }
}
