package com.kone.assistant

import android.Manifest
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kone.assistant.audio.AudioCapture
import com.kone.assistant.audio.CaptureResult
import com.kone.assistant.audio.MicrophoneForegroundService
import com.kone.assistant.ptt.PushToTalkController
import com.kone.assistant.stt.SttEvent
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var capture: AudioCapture
    private val stopHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AudioCaptureScreen() } }
    }

    override fun onDestroy() {
        stopHandler.removeCallbacksAndMessages(null)
        if (::capture.isInitialized) capture.stop()
        super.onDestroy()
    }

    @Composable
    private fun AudioCaptureScreen() {
        var hasPermission by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        }
        var permissionRequested by remember { mutableStateOf(false) }
        var notificationPermissionGranted by remember {
            mutableStateOf(
                Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )
        }
        var recording by remember { mutableStateOf(false) }
        var level by remember { mutableFloatStateOf(0f) }
        var elapsedMs by remember { mutableLongStateOf(0L) }
        var status by remember { mutableStateOf("Mikrofon izni bekleniyor") }
        var modelStatus by remember { mutableStateOf("Model servisi başlatılmadı") }
        var partialText by remember { mutableStateOf("") }
        var finalText by remember { mutableStateOf("") }
        var finalLatencyMs by remember { mutableLongStateOf(0L) }
        var pttRecording by remember { mutableStateOf(false) }
        var intentText by remember { mutableStateOf("—") }
        var actionText by remember { mutableStateOf("—") }
        var sampleNumber by remember { mutableIntStateOf(existingSampleCount() + 1) }
        val results = remember { mutableStateListOf<CaptureResult>() }

        val pushToTalk = remember {
            PushToTalkController(
                context = this,
                onSttEvent = { event ->
                    runOnUiThread {
                        when (event) {
                            is SttEvent.ModelLoading -> modelStatus = event.message
                            is SttEvent.ModelReady -> modelStatus = "Model hazır · ${event.loadMs} ms"
                            is SttEvent.Partial -> partialText = event.text
                            is SttEvent.Final -> {
                                finalText = event.text.ifBlank { "(anlaşılan kelime yok)" }
                                partialText = ""
                                finalLatencyMs = event.latencyMs
                            }
                            is SttEvent.Error -> modelStatus = "STT hatası: ${event.message}"
                        }
                    }
                },
                onProgress = { progress -> runOnUiThread { level = progress.level; elapsedMs = progress.elapsedMs } },
                onPipelineResult = { result ->
                    runOnUiThread {
                        intentText = "${result.intent.id} · confidence ${"%.2f".format(Locale.US, result.intent.confidence.score)} · ${result.intent.reason.code}" +
                            result.intent.slots.joinToString(prefix = if (result.intent.slots.isEmpty()) "" else " · ") { "${it.name}=${it.value}" }
                        actionText = result.action.summary
                    }
                },
                onError = { message -> runOnUiThread { status = message; pttRecording = false } },
            )
        }

        capture = remember {
            AudioCapture(
                outputDirectory = File(filesDir, "test_audio"),
                onProgress = { progress ->
                    runOnUiThread {
                        level = progress.level
                        elapsedMs = progress.elapsedMs
                    }
                },
                onFinished = { result ->
                    runOnUiThread {
                        recording = false
                        level = 0f
                        results.add(0, result)
                        if (result.file.name.startsWith("sample_")) sampleNumber = existingSampleCount() + 1
                        status = analysisText(result)
                    }
                },
                onError = { message -> runOnUiThread { recording = false; status = message } },
            )
        }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            permissionRequested = true
            status = if (granted) "İzin verildi; kayıt hazır" else "Mikrofon izni reddedildi; kayıt başlatılamaz"
        }
        val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionGranted = granted
            status = if (granted) {
                "Bildirim izni verildi; kilit ekranı dinlemesini başlatabilirsin"
            } else {
                "Bildirim izni reddedildi; görünür servis başlatılmadı"
            }
        }

        DisposableEffect(Unit) { onDispose { if (capture.isRecording) capture.stop() } }
        DisposableEffect(pushToTalk) { onDispose { pushToTalk.close() } }
        DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val text = intent?.getStringExtra(MicrophoneForegroundService.EXTRA_STT_TEXT).orEmpty()
                    when (intent?.getStringExtra(MicrophoneForegroundService.EXTRA_STT_TYPE)) {
                        "model_loading" -> modelStatus = text
                        "model_ready" -> modelStatus = "$text · ${intent.getLongExtra(MicrophoneForegroundService.EXTRA_STT_VALUE, 0)} ms"
                        "partial" -> partialText = text
                        "final" -> {
                            finalText = text.ifBlank { "(anlaşılan kelime yok)" }
                            partialText = ""
                            finalLatencyMs = intent.getLongExtra(MicrophoneForegroundService.EXTRA_STT_VALUE, 0)
                        }
                        "error" -> modelStatus = "STT hatası: $text"
                    }
                }
            }
            ContextCompat.registerReceiver(
                this@MainActivity,
                receiver,
                IntentFilter(MicrophoneForegroundService.ACTION_STT_EVENT),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose { unregisterReceiver(receiver) }
        }

        fun startCapture(fileName: String, autoStopMs: Long? = null) {
            if (!hasPermission) {
                permissionRequested = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
            elapsedMs = 0
            level = 0f
            status = "16 kHz mono PCM kaydediliyor"
            capture.start(fileName)
            recording = capture.isRecording
            autoStopMs?.let { stopHandler.postDelayed({ capture.stop() }, it) }
        }

        fun startForegroundListening() {
            if (!hasPermission) {
                permissionRequested = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
            if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= 33) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            val serviceIntent = Intent(this, MicrophoneForegroundService::class.java)
                .setAction(MicrophoneForegroundService.ACTION_START)
            ContextCompat.startForegroundService(this, serviceIntent)
            status = "Foreground mikrofon servisi başlatılıyor; bildirimi kontrol et"
        }

        AudioScreenContent(
            hasPermission = hasPermission,
            permissionPermanentlyDenied = permissionRequested && !hasPermission &&
                !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO),
            recording = recording,
            level = level,
            elapsedMs = elapsedMs,
            status = status,
            modelStatus = modelStatus,
            partialText = partialText,
            finalText = finalText,
            finalLatencyMs = finalLatencyMs,
            pttRecording = pttRecording,
            intentText = intentText,
            actionText = actionText,
            nextSample = sampleNumber,
            results = results,
            onRequestPermission = { permissionRequested = true; permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onOpenSettings = {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            },
            onStartSample = { if (sampleNumber <= 5) startCapture("sample_$sampleNumber.pcm") },
            onStartStability = { startCapture("stability_30s.pcm", 30_000) },
            onStop = { stopHandler.removeCallbacksAndMessages(null); capture.stop() },
            onPttPress = {
                elapsedMs = 0
                level = 0f
                finalText = ""
                intentText = "—"
                actionText = "—"
                status = "Dinliyorum; konuş ve düğmeyi bırak"
                pushToTalk.press()
                pttRecording = pushToTalk.isRecording
            },
            onPttRelease = {
                pushToTalk.release()
                pttRecording = false
                level = 0f
                status = "Konuşma işleniyor"
            },
            onStartForegroundService = { startForegroundListening() },
            onStopForegroundService = {
                stopService(Intent(this, MicrophoneForegroundService::class.java))
                status = "Foreground mikrofon servisi durduruldu"
            },
        )
    }

    private fun existingSampleCount(): Int =
        File(filesDir, "test_audio").listFiles()?.count { it.name.matches(Regex("sample_[1-5]\\.pcm")) } ?: 0

    private fun analysisText(result: CaptureResult): String = String.format(
        Locale.US,
        "%s · RMS %.3f · clipping %.3f%% · buffer %.1f%% · hata %d",
        if (result.isSilent) "SESSİZLİK" else "SES VAR",
        result.rms,
        result.clippingPercent,
        result.bufferCoveragePercent,
        result.readErrors,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AudioScreenContent(
    hasPermission: Boolean,
    permissionPermanentlyDenied: Boolean,
    recording: Boolean,
    level: Float,
    elapsedMs: Long,
    status: String,
    modelStatus: String,
    partialText: String,
    finalText: String,
    finalLatencyMs: Long,
    pttRecording: Boolean,
    intentText: String,
    actionText: String,
    nextSample: Int,
    results: List<CaptureResult>,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSample: () -> Unit,
    onStartStability: () -> Unit,
    onStop: () -> Unit,
    onPttPress: () -> Unit,
    onPttRelease: () -> Unit,
    onStartForegroundService: () -> Unit,
    onStopForegroundService: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Kone · PCM Testi") }) }) { inset ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inset).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("16 kHz · mono · PCM 16-bit", style = MaterialTheme.typography.titleMedium)
            Text(status)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Türkçe çevrimdışı STT", style = MaterialTheme.typography.labelLarge)
                    Text(modelStatus)
                    Text("Geçici: ${partialText.ifBlank { "—" }}")
                    Text("Sonuç: ${finalText.ifBlank { "—" }}")
                    if (finalLatencyMs > 0) Text("Sonuç gecikmesi: $finalLatencyMs ms")
                    Text("Intent: $intentText")
                    Text("Sahte action: $actionText")
                }
            }

            if (!hasPermission) {
                Button(onClick = if (permissionPermanentlyDenied) onOpenSettings else onRequestPermission) {
                    Text(if (permissionPermanentlyDenied) "Uygulama ayarlarını aç" else "Mikrofon izni ver")
                }
            } else {
                Text("Süre: %.1f sn".format(Locale.US, elapsedMs / 1000.0))
                LinearProgressIndicator(progress = { level }, modifier = Modifier.fillMaxWidth())
                LevelWave(level)
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().pointerInput(onPttPress, onPttRelease) {
                        detectTapGestures(
                            onPress = {
                                onPttPress()
                                tryAwaitRelease()
                                onPttRelease()
                            }
                        )
                    },
                ) {
                    Text(if (pttRecording) "Dinliyorum… bırak" else "Konuşmak için basılı tut")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recording) {
                        Button(onClick = onStop) { Text("Durdur") }
                    } else {
                        Button(onClick = onStartSample, enabled = nextSample <= 5) { Text("Örnek $nextSample başlat") }
                        Button(onClick = onStartStability) { Text("30 sn test") }
                    }
                }
            }

            Text("Kayıtlar uygulamanın özel test_audio klasöründe ham .pcm olarak tutulur.")
            results.take(5).forEach { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(result.file.name, style = MaterialTheme.typography.labelLarge)
                        Text("${result.durationMs} ms · ${result.bytesWritten} byte")
                        Text("Sessiz: ${result.isSilent} · clipping: ${"%.3f".format(Locale.US, result.clippingPercent)}% · hata: ${result.readErrors}")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LevelWave(level: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val center = size.height / 2
        val amplitude = level * center
        val step = size.width / 32
        for (index in 0..32) {
            val scale = if (index % 2 == 0) 1f else 0.55f
            drawLine(
                color = if (level > 0.9f) Color.Red else Color(0xFF3451B2),
                start = Offset(index * step, center - amplitude * scale),
                end = Offset(index * step, center + amplitude * scale),
                strokeWidth = 4f,
            )
        }
    }
}
