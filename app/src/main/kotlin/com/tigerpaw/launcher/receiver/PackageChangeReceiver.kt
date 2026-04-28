package com.tigerpaw.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives package-change broadcasts (install / remove / replace) from the OS.
 * The app drawer's [AppRepository] is backed by a cold Flow; consumers that are
 * alive will re-query via their ViewModel when they return to the foreground.
 * This receiver exists primarily so the system delivers the intents while the
 * launcher is the active HOME app.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No local state to invalidate — ViewModels re-load on next subscription.
        // Future enhancement: broadcast a local event to trigger an immediate refresh.
    }
}
