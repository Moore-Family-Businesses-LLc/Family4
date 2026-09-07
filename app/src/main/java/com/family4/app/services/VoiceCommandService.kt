package com.family4.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.family4.app.R
import com.family4.app.ui.main.MainActivity
import java.util.Locale

/**
 * Always-listening voice command service.
 *
 * Wake phrase: "family four" or "family 4"
 * After wake → TTS says "I'm Ready" → enters command mode for 8 seconds
 * Commands: "open [screen]", "add task [title]", "create event [title]"
 *
 * Lifecycle: start/stop via [ACTION_START] / [ACTION_STOP] intents.
 */
class VoiceCommandService : Service() {

    companion object {
        const val ACTION_START = "com.family4.app.voice.START"
        const val ACTION_STOP  = "com.family4.app.voice.STOP"

        const val CHANNEL_ID   = "voice_listening"
        const val NOTIF_ID     = 9001

        private val WAKE_PHRASES = setOf("family four", "family 4", "family for")
        private const val TAG = "VoiceCommandService"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isListeningForCommand = false
    private var restartPending = false

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        initTts()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIF_ID, buildNotification("Listening for \"Family Four\"…"))
                startListening()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListening()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    // ── TTS ────────────────────────────────────────────────────────────────────

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        val utteranceId = "vc_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { onDone?.invoke() }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {}
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    // ── Speech Recognition ────────────────────────────────────────────────────

    private fun startListening() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
        speechRecognizer?.setRecognitionListener(recognitionListener)
        speechRecognizer?.startListening(buildRecognizerIntent())
        Log.d(TAG, "Listening started")
    }

    private fun stopListening() {
        restartPending = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun restartListening() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        startListening()
    }

    private fun buildRecognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            handleResults(matches)
            // Restart loop after short delay so TTS finishes before next listen
            android.os.Handler(mainLooper).postDelayed({ restartListening() }, 500)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            // Check for wake phrase in partial results for faster response
            if (!isListeningForCommand) {
                val text = partial?.firstOrNull()?.lowercase(Locale.getDefault()) ?: return
                if (WAKE_PHRASES.any { text.contains(it) }) {
                    onWakePhraseDetected()
                }
            }
        }

        override fun onError(error: Int) {
            Log.d(TAG, "Speech error: $error")
            // Auto-restart unless the service is being stopped
            android.os.Handler(mainLooper).postDelayed({ restartListening() }, 1000)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleResults(matches: List<String>?) {
        val texts = matches ?: return
        val combined = texts.joinToString(" ").lowercase(Locale.getDefault())

        if (!isListeningForCommand) {
            if (WAKE_PHRASES.any { combined.contains(it) }) {
                onWakePhraseDetected()
            }
        } else {
            isListeningForCommand = false
            processCommand(combined)
        }
    }

    private fun onWakePhraseDetected() {
        isListeningForCommand = true
        updateNotification("Listening for command…")
        speak("I'm Ready") {
            // Reset command mode after 8 seconds if no command received
            android.os.Handler(mainLooper).postDelayed({
                isListeningForCommand = false
                updateNotification("Listening for \"Family Four\"…")
            }, 8000)
        }
    }

    private fun processCommand(text: String) {
        Log.d(TAG, "Command: $text")
        updateNotification("Listening for \"Family Four\"…")

        val navTarget = when {
            text.contains("open calendar") || text.contains("calendar") -> "calendar"
            text.contains("open chat")     || text.contains("chat")     -> "chat"
            text.contains("open notes")    || text.contains("notes")    -> "notes"
            text.contains("open dashboard") || text.contains("home")    -> "dashboard"
            text.contains("open tasks")    || text.contains("tasks")    -> "tasks"
            text.contains("open board")    || text.contains("board")    -> "board"
            text.contains("open map")      || text.contains("map")      -> "map"
            text.contains("open weather")  || text.contains("weather")  -> "weather"
            text.contains("open albums")   || text.contains("photos")   -> "albums"
            else -> null
        }

        if (navTarget != null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("voice_nav_target", navTarget)
            }
            startActivity(intent)
            speak("Opening $navTarget")
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Commands",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always-on voice activation for Family4"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, VoiceCommandService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Family4 — Voice Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_close_white, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}
