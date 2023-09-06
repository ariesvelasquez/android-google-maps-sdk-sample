package com.ariesvelasquez.androidmaps

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*

class LocationUtil(
    context: Context,
    private var locationCallback: LocationCallback,
    private var timeInterval: Long = 5000,
    private var minimalDistance: Float  = 2f
) {

    private var request: LocationRequest
    private var locationClient: FusedLocationProviderClient

    init {
        // getting the location client
        locationClient = LocationServices.getFusedLocationProviderClient(context)
        request = createRequest()
    }

    private fun createRequest(): LocationRequest =
        // New builder
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, timeInterval).apply {
            setMinUpdateDistanceMeters(minimalDistance)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

    fun changeRequest(timeInterval: Long, minimalDistance: Float) {
        this.timeInterval = timeInterval
        this.minimalDistance = minimalDistance
        createRequest()
        stopLocationTracking()
        startLocationTracking()
    }

    @SuppressLint("MissingPermission")
    fun startLocationTracking() =
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())


    fun stopLocationTracking() {
        locationClient.flushLocations()
        locationClient.removeLocationUpdates(locationCallback)
    }

    fun restartLocationTracking() {
        stopLocationTracking()
        startLocationTracking()
    }
}
