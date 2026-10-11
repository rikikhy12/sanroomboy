package com.quickclean.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        onLocationResult: (latitude: Double, longitude: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Izin lokasi belum diberikan. Mohon izinkan akses lokasi.")
            return
        }

        if (!isGpsEnabled(context)) {
            onError("GPS perangkat Anda nonaktif. Mohon aktifkan GPS lokasi Anda.")
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000
            ).apply {
                setMinUpdateIntervalMillis(1000)
                setMaxUpdates(1)
            }.build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val location = result.lastLocation
                    if (location != null) {
                        fusedClient.removeLocationUpdates(this)
                        onLocationResult(location.latitude, location.longitude)
                    }
                }
            }

            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            // Also check last known location for instant response
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    onLocationResult(loc.latitude, loc.longitude)
                }
            }

            // Fallback to Android native LocationManager
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager?.let { lm ->
                val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val best = gpsLoc ?: netLoc
                if (best != null) {
                    onLocationResult(best.latitude, best.longitude)
                }
            }

        } catch (e: Exception) {
            onError("Gagal mengambil koordinat GPS: ${e.localizedMessage}")
        }
    }
}
