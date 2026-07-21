package com.kone.assistant.audio

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kone.assistant.MainActivity
import com.kone.assistant.audio.vad.RmsVoiceActivityDetector
import com.kone.assistant.audio.vad.SpeechSegmenter
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

class MicrophoneForegroundService : Service() {
    enum class State { IDLE, STARTING, RECORDING, STOPPING, ERROR }

    companion object {
        const val ACTION_START = "com.kone.assistant.action.START_MICROPHONE"
        const val ACTION_STOP = "com.kone.assistant.action.STOP_MICROPHONE"
        const val CHANNEL_ID = "microphone_capture"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "MicForegroundService"

        @Volatile
        var currentState: State = State.IDLE
            private set
    }

    private lateinit var capture: AudioCapture
    private val lifecycleLog by lazy { File(filesDir, "lifecycle/foreground_service.log") }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        transition(State.IDLE, "onCreate")
        val segmenter = SpeechSegmenter(RmsVoiceActivityDetector()) { segment ->
            val directory = File(filesDir, "speech_segments").apply { mkdirs() }
            val output = File(directory, "speech_${System.currentTimeMillis()}_${segment.startedAtMs}-${segment.endedAtMs}.pcm")
            FileOutputStream(output).use { stream ->
                val bytes = ByteArray(segment.pcm.size * 2)
                segment.pcm.forEachIndexed { index, sample ->
                    val value = sample.toInt()
                    bytes[index * 2] = (value and 0xff).toByte()
                    bytes[index * 2 + 1] = ((value ushr 8) and 0xff).toByte()
                }
                stream.write(bytes)
            }
            logEvent("speech_segment file=${output.name} startMs=${segment.startedAtMs} endMs=${segment.endedAtMs} noise=${segment.noiseFloorRms} threshold=${segment.thresholdRms}")
            // This callback is the hand-off point for a future STT engine.
        }
        capture = AudioCapture(
            outputDirectory = File(filesDir, "foreground_audio"),
            onProgress = { },
            onFinished = { result ->
                logEvent("capture_finished durationMs=${result.durationMs} bytes=${result.bytesWritten} readErrors=${result.readErrors}")
            },
            onError = { message ->
                transition(State.ERROR, "capture_error=$message")
                stopSafely("capture_error")
            },
            speechSegmenter = segmenter,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logEvent("onStartCommand action=${intent?.action ?: "null"} startId=$startId")
        when (intent?.action) {
            ACTION_STOP -> stopSafely("notification_action")
            ACTION_START -> startCapture()
            else -> stopSafely("missing_action")
        }
        return START_NOT_STICKY
    }

    private fun startCapture() {
        if (currentState == State.RECORDING || currentState == State.STARTING) {
            logEvent("start_ignored state=$currentState")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            transition(State.ERROR, "record_audio_not_granted")
            stopSafely("permission_missing")
            return
        }

        transition(State.STARTING, "user_visible_start")
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
            val fileName = "foreground_${System.currentTimeMillis()}.pcm"
            capture.start(fileName)
            if (capture.isRecording) {
                transition(State.RECORDING, "audio_record_started file=$fileName")
            } else {
                transition(State.ERROR, "audio_record_not_started")
                stopSafely("start_failed")
            }
        } catch (error: Exception) {
            transition(State.ERROR, "foreground_start_error=${error.javaClass.simpleName}")
            stopSafely("foreground_exception")
        }
    }

    private fun stopSafely(reason: String) {
        if (currentState != State.IDLE) transition(State.STOPPING, "reason=$reason")
        if (::capture.isInitialized && capture.isRecording) capture.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        transition(State.IDLE, "stopped")
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MicrophoneForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Kone mikrofonu dinliyor")
            .setContentText("16 kHz mono PCM · uygulamayı açmak için dokunun")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "DİNLEMEYİ DURDUR", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Mikrofon dinleme",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Aktif mikrofon foreground service durumu"
                setShowBadge(false)
            }
        )
    }

    private fun transition(next: State, reason: String) {
        val previous = currentState
        currentState = next
        logEvent("state=$previous->$next reason=$reason")
    }

    private fun logEvent(message: String) {
        val line = "${Instant.now()} $message"
        Log.i(TAG, line)
        runCatching {
            lifecycleLog.parentFile?.mkdirs()
            lifecycleLog.appendText("$line\n")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        logEvent("onTaskRemoved state=$currentState")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        logEvent("onDestroy state=$currentState")
        if (::capture.isInitialized && capture.isRecording) capture.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        currentState = State.IDLE
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
