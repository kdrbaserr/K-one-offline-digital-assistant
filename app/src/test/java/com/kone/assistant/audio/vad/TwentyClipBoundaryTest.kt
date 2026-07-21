package com.kone.assistant.audio.vad

import java.io.File
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TwentyClipBoundaryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun `twenty quiet speech and wind clips have expected boundaries`() {
        val cases = buildList {
            repeat(7) { add(ClipCase("quiet_${it + 1}", "quiet", 80 + it * 10, false)) }
            repeat(7) { add(ClipCase("speech_${it + 1}", "speech", 80 + it * 10, true)) }
            repeat(3) { add(ClipCase("wind_${it + 1}", "wind", 650 + it * 50, false)) }
            repeat(3) { add(ClipCase("wind_speech_${it + 1}", "wind", 650 + it * 50, true)) }
        }
        assertEquals(20, cases.size)

        val analyzer = PcmClipBoundaryAnalyzer()
        cases.forEach { case ->
            val file = File(temporaryFolder.root, "${case.name}.pcm")
            file.writeBytes(createClip(case).toLittleEndianBytes())
            val report = analyzer.analyze(file, case.category)
            if (!case.hasSpeech) {
                assertTrue("${case.name} produced false speech", report.segments.isEmpty())
            } else {
                assertEquals("${case.name} segment count", 1, report.segments.size)
                val segment = report.segments.single()
                assertTrue("${case.name} start=${segment.startedAtMs}", segment.startedAtMs in 1_080..1_120)
                assertTrue("${case.name} end=${segment.endedAtMs}", segment.endedAtMs in 2_680..2_720)
            }
        }
    }

    private fun createClip(case: ClipCase): ShortArray {
        val sampleRate = 16_000
        return ShortArray(sampleRate * 3) { index ->
            val base = sin(index * 2.0 * Math.PI / 73.0) * case.noiseAmplitude
            val speech = if (case.hasSpeech && index in sampleRate * 14 / 10 until sampleRate * 22 / 10) {
                sin(index * 2.0 * Math.PI / 32.0) * 5_000
            } else 0.0
            (base + speech).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun ShortArray.toLittleEndianBytes(): ByteArray = ByteArray(size * 2).also { bytes ->
        forEachIndexed { index, sample ->
            val value = sample.toInt()
            bytes[index * 2] = (value and 0xff).toByte()
            bytes[index * 2 + 1] = ((value ushr 8) and 0xff).toByte()
        }
    }

    private data class ClipCase(
        val name: String,
        val category: String,
        val noiseAmplitude: Int,
        val hasSpeech: Boolean,
    )
}
