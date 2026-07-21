package com.kone.assistant.benchmark

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kone.assistant.stt.VoskModelAssetInstaller
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.vosk.Model
import org.vosk.Recognizer

@RunWith(AndroidJUnit4::class)
class VoskClipBenchmarkTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun sixtyClips() {
        val context = instrumentation.targetContext
        val clips = File(context.filesDir, "benchmark_clips").listFiles { file -> file.extension == "pcm" }
            ?.sortedBy { it.name }
            ?.take(60)
            .orEmpty()
        assertEquals("Expected 60 benchmark PCM clips", 60, clips.size)

        val output = File(context.filesDir, "benchmark/vosk-raw.csv")
        output.parentFile?.mkdirs()
        output.writeText("engine,clip,audio_ms,latency_ms,pss_kb,temp_c,transcript\n")
        val modelPath = VoskModelAssetInstaller(context).install()
        Model(modelPath.absolutePath).use { model ->
            clips.forEach { clip ->
                val pcm = clip.readBytes()
                val started = SystemClock.elapsedRealtime()
                val transcript = Recognizer(model, 16_000f).use { recognizer ->
                    recognizer.acceptWaveForm(pcm, pcm.size)
                    JSONObject(recognizer.finalResult).optString("text").trim()
                }
                val latency = SystemClock.elapsedRealtime() - started
                output.appendText(
                    listOf(
                        "vosk-small-tr-0.3",
                        clip.name,
                        (pcm.size / 32).toString(),
                        latency.toString(),
                        Debug.getPss().toString(),
                        batteryTemperatureC().toString(),
                        csv(transcript),
                    ).joinToString(",") + "\n"
                )
            }
        }
    }

    private fun batteryTemperatureC(): Double {
        val battery = instrumentation.targetContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10.0) ?: 0.0
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
