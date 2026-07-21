package com.kone.assistant.audio.vad

import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RmsVadTest {
    @Test fun `calibration learns quiet device baseline`() {
        val vad = RmsVoiceActivityDetector(RmsVadConfig(calibrationMs = 40))
        val quiet = ShortArray(320) { 160 }
        assertTrue(vad.analyze(quiet).isCalibrating)
        val calibrated = vad.analyze(quiet)
        assertFalse(calibrated.isSpeech)
        assertTrue(calibrated.noiseFloorRms in 0.004..0.006)
    }

    @Test fun `speech rises above learned baseline`() {
        val vad = RmsVoiceActivityDetector(RmsVadConfig(calibrationMs = 20))
        vad.analyze(ShortArray(320) { 100 })
        val decision = vad.analyze(tone(amplitude = 5_000))
        assertTrue(decision.isSpeech)
        assertTrue(decision.rms > decision.thresholdRms)
    }

    @Test fun `pre roll and post roll preserve utterance edges`() {
        val segments = mutableListOf<SpeechSegment>()
        val segmenter = SpeechSegmenter(
            detector = RmsVoiceActivityDetector(RmsVadConfig(calibrationMs = 100)),
            config = SpeechSegmenterConfig(preRollMs = 100, postRollMs = 100, startFrames = 1),
            onSegment = segments::add,
        )
        repeat(10) { segmenter.accept(ShortArray(320) { 80 }) }
        repeat(10) { segmenter.accept(tone(amplitude = 6_000)) }
        repeat(5) { segmenter.accept(ShortArray(320) { 80 }) }
        assertEquals(1, segments.size)
        assertEquals(100L, segments.single().startedAtMs)
        assertEquals(500L, segments.single().endedAtMs)
    }

    @Test fun `steady wind-like noise does not become speech after calibration`() {
        val vad = RmsVoiceActivityDetector(RmsVadConfig(calibrationMs = 100))
        val wind = tone(amplitude = 700)
        repeat(5) { vad.analyze(wind) }
        assertFalse(vad.analyze(wind).isSpeech)
    }

    private fun tone(amplitude: Int): ShortArray = ShortArray(320) { index ->
        (sin(index * 2.0 * Math.PI / 32.0) * amplitude).toInt().toShort()
    }
}
