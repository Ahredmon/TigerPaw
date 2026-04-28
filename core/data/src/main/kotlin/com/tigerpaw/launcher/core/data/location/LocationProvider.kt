package com.tigerpaw.launcher.core.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Returns the best last-known location if either fine or coarse location permission is
     * granted; otherwise returns null. No active location fix is requested — this is a
     * best-effort, low-latency read used to contextualise app-launch suggestions.
     */
    fun getLastLocation(): Location? {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }
}
