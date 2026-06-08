package com.featherframe.app.domain.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * FusedLocationProviderClient wrapper for geotagging bird captures with GPS coordinates.
 */
class GPSManager(private val context: Context) {

    companion object {
        private const val TAG = "GPSManager"
        private const val LOCATION_TIMEOUT_MS = 10_000L
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Data class representing a GPS location with coordinates.
     */
    data class GpsLocation(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double? = null,
        val accuracy: Float? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Check if fine location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the current GPS location. Requires location permission.
     * This is a simplified single-shot location fetch.
     *
     * @return The current GpsLocation, or null if unavailable/permission denied
     */
    suspend fun getCurrentLocation(): GpsLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            android.util.Log.w(TAG, "Location permission not granted")
            return@withContext null
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()

            val location = suspendCancellableCoroutine<Location?> { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc ->
                    continuation.resume(loc)
                }.addOnFailureListener { exception ->
                    android.util.Log.e(TAG, "Failed to get location", exception)
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }

            location?.let {
                GpsLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = if (it.hasAltitude()) it.altitude else null,
                    accuracy = if (it.hasAccuracy()) it.accuracy else null,
                    timestamp = it.time
                )
            }
        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "Security exception getting location", e)
            null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting location", e)
            null
        }
    }

    /**
     * Get the last known location (faster, less accurate).
     */
    suspend fun getLastKnownLocation(): GpsLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext null
        }

        try {
            val location = suspendCancellableCoroutine<Location?> { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { loc ->
                        continuation.resume(loc)
                    }
                    .addOnFailureListener { exception ->
                        android.util.Log.e(TAG, "Failed to get last location", exception)
                        continuation.resume(null)
                    }
            }

            location?.let {
                GpsLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = if (it.hasAltitude()) it.altitude else null,
                    accuracy = if (it.hasAccuracy()) it.accuracy else null,
                    timestamp = it.time
                )
            }
        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "Security exception", e)
            null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting last location", e)
            null
        }
    }
}
