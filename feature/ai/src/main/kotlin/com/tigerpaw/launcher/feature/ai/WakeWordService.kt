package com.tigerpaw.launcher.feature.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig

private const val TAG = "WakeWordService"
private const val NOTIF_CHANNEL = "wake_word"
private const val NOTIF_ID = 42
private const val SAMPLE_RATE = 16_000

/**
 * Foreground service that runs Sherpa-ONNX keyword spotting in the background.
 *
 * Model assets are expected under `assets/wake_word/`:
 *   encoder.onnx, decoder.onnx, joiner.onnx, tokens.txt, keywords.txt
 *
 * When the wake word is detected a local broadcast [ACTION_DETECTED] is sent.
 * The launcher's home screen / orb listen for this to open the assistant.
 */
class WakeWordService : Service() {

    companion object {
        const val ACTION_DETECTED = "com.tigerpaw.launcher.WAKE_WORD_DETECTED"
    }

    private var spotter: KeywordSpotter? = null
    private var audioRecord: AudioRecord? = null
    @Volatile private var running = false

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) startSpotter()
        return START_STICKY
    }

    override fun onDestroy() {
        stopSpotter()
        super.onDestroy()
    }

    // ── Spotter init ───────────────────────────────────────────────────────

    private fun startSpotter() {
        try {
            val config = KeywordSpotterConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = "wake_word/encoder.onnx",
                        decoder = "wake_word/decoder.onnx",
                        joiner = "wake_word/joiner.onnx",
                    ),
                    tokens = "wake_word/tokens.txt",
                    modelType = "zipformer2",
                ),
                keywordsFile = "wake_word/keywords.txt",
            )
            spotter = KeywordSpotter(assets, config)
            running = true
            Thread(::micLoop, "wake-word-mic").start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialise KeywordSpotter: ${e.message}", e)
            stopSelf()
        }
    }

    private fun stopSpotter() {
        running = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        spotter?.release()
        spotter = null
    }

    // ── Mic loop ───────────────────────────────────────────────────────────

    private fun micLoop() {
        val kws = spotter ?: return
        val stream = kws.createStream()

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2,
        )
        audioRecord = record
        record.startRecording()

        val buf = ShortArray(512)
        while (running) {
            val n = record.read(buf, 0, buf.size)
            if (n <= 0) continue

            val samples = FloatArray(n) { buf[it] / 32768f }
            stream.acceptWaveform(samples, sampleRate = SAMPLE_RATE)

            while (kws.isReady(stream)) {
                kws.decode(stream)
                val result = kws.getResult(stream)
                if (result.keyword.isNotBlank()) {
                    Log.d(TAG, "Wake word detected: '${result.keyword}'")
                    sendBroadcast(Intent(ACTION_DETECTED).apply {
                        setPackage(packageName)
                    })
                    kws.reset(stream)
                }
            }
        }

        record.stop()
        record.release()
        stream.release()
    }

    // ── Notification ───────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(NOTIF_CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    NOTIF_CHANNEL,
                    "Wake Word",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Listening for the wake word in the background" }
            )
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) }

        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("TigerPaw")
            .setContentText("Listening for wake word…")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .apply { tapIntent?.let { setContentIntent(it) } }
            .build()
    }
}
