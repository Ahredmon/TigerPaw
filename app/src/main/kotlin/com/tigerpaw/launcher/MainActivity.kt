package com.tigerpaw.launcher

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tigerpaw.launcher.core.data.prefs.LauncherPreferences
import com.tigerpaw.launcher.core.ui.theme.TigerPawTheme
import com.tigerpaw.launcher.feature.ai.WakeWordService
import com.tigerpaw.launcher.navigation.TigerPawNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "TigerPaw/Main"

        /** Cooldown between consecutive shake triggers (ms). */
        private const val SHAKE_COOLDOWN_MS = 1_500L
        /** Minimum poll interval for shake processing (ms). */
        private const val SHAKE_POLL_MS = 80L

        /**
         * Map sensitivity slider (0..1) → jerk threshold.
         * Higher sensitivity value → lower threshold → triggers more easily.
         * Range: sensitivity=0 → threshold=1800, sensitivity=1 → threshold=200.
         */
        fun sensitivityToThreshold(sensitivity: Float): Float =
            1800f - sensitivity.coerceIn(0f, 1f) * 1600f
    }

    @Inject lateinit var prefs: LauncherPreferences

    private val appViewModel: AppViewModel by viewModels()

    // ── Shake detection ─────────────────────────────────────────────────────

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastShakeMs = 0L
    private var lastPollMs = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    // Shake pref values read on each sensor event (no coroutine overhead on the hot path).
    @Volatile private var shakeEnabled = true
    @Volatile private var shakeThreshold = sensitivityToThreshold(0.5f)

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!shakeEnabled) return
            val now = System.currentTimeMillis()
            val elapsed = now - lastPollMs
            if (elapsed < SHAKE_POLL_MS) return
            lastPollMs = now

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val jerk = kotlin.math.abs(x + y + z - lastX - lastY - lastZ) / elapsed * 10_000
            lastX = x; lastY = y; lastZ = z

            if (jerk > shakeThreshold && now - lastShakeMs > SHAKE_COOLDOWN_MS) {
                lastShakeMs = now
                Log.d(TAG, "shake detected jerk=${"%.0f".format(jerk)} threshold=${"%.0f".format(shakeThreshold)}")
                appViewModel.requestOpenAssistant()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    // ────────────────────────────────────────────────────────────────────────

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate — fresh=${savedInstanceState == null}")

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Handle ASSIST intent delivered at cold-start
        handleIntent(intent)

        // Register wake word receiver for the full activity lifetime.
        val filter = IntentFilter(WakeWordService.ACTION_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeWordReceiver, filter)
        }

        enableEdgeToEdge()
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContent {
            TigerPawTheme {
                // Fullscreen mode — hide/show status bar reactively.
                val fullscreen by prefs.fullscreen.collectAsState(initial = false)
                LaunchedEffect(fullscreen) {
                    val insetsController = window.insetsController
                    if (insetsController != null) {
                        if (fullscreen) {
                            insetsController.hide(android.view.WindowInsets.Type.statusBars())
                            insetsController.systemBarsBehavior =
                                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            insetsController.show(android.view.WindowInsets.Type.statusBars())
                        }
                    }
                }

                // Request runtime permissions on first launch.
                val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.READ_MEDIA_IMAGES
                else
                    Manifest.permission.READ_EXTERNAL_STORAGE
                val permissionsState = rememberMultiplePermissionsState(
                    permissions = listOf(
                        storagePermission,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECORD_AUDIO,
                    ),
                )
                LaunchedEffect(Unit) {
                    if (!permissionsState.allPermissionsGranted) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    TigerPawNavHost(openAssistantTrigger = appViewModel.openAssistant)
                }
            }
        }
    }

    /** Called when the activity is re-used by the system (e.g. ASSIST long-press while running). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_ASSIST -> {
                Log.i(TAG, "handleIntent: ACTION_ASSIST received")
                appViewModel.requestOpenAssistant()
            }
            WakeWordService.ACTION_DETECTED -> {
                Log.i(TAG, "handleIntent: wake word intent received")
                appViewModel.requestOpenAssistant()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        // Snapshot shake prefs so the hot sensor callback can read them without a coroutine.
        runBlocking {
            shakeEnabled = prefs.shakeEnabled.first()
            shakeThreshold = sensitivityToThreshold(prefs.shakeSensitivity.first())
        }
        Log.d(TAG, "shake enabled=$shakeEnabled threshold=$shakeThreshold")
        accelerometer?.also { sensor ->
            sensorManager.registerListener(shakeListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        // (Re-)sync wake word service state after returning from Settings.
        runBlocking {
            syncWakeWordService(prefs.wakeWordEnabled.first())
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        sensorManager.unregisterListener(shakeListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        runCatching { unregisterReceiver(wakeWordReceiver) }
    }

    // ── Wake word service helpers ─────────────────────────────────────────────

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WakeWordService.ACTION_DETECTED) {
                Log.i(TAG, "wake word detected")
                // Bring launcher to front if it's not the active window.
                val bringToFront = Intent(context, MainActivity::class.java).apply {
                    action = WakeWordService.ACTION_DETECTED
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(bringToFront)
                appViewModel.requestOpenAssistant()
            }
        }
    }

    private fun syncWakeWordService(enabled: Boolean) {
        val serviceIntent = Intent(this, WakeWordService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            stopService(serviceIntent)
        }
    }
}
