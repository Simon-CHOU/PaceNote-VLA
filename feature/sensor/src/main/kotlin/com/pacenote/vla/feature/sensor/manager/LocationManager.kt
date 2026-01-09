package com.pacenote.vla.feature.sensor.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pacenote.vla.core.domain.model.TelemetryData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages GPS location updates with high precision for driving scenarios
 * Target: 1Hz (1 second) for GPS, fused with IMU at 50Hz
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L // 1 second interval
    ).apply {
        setMinUpdateIntervalMillis(500L)
        setMinUpdateDistanceMeters(1.0f)
        // setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL) // Not available in current SDK
        setWaitForAccurateLocation(true)
    }.build()

    /**
     * Location updates flow
     */
    fun locationFlow(): Flow<TelemetryData> = callbackFlow {
        if (!hasLocationPermission()) {
            close(IllegalArgumentException("Location permission not granted"))
            return@callbackFlow
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        TelemetryData(
                            timestamp = location.time,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            speedMps = location.speed,
                            bearing = location.bearing,
                            accuracy = location.accuracy
                        )
                    )
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
