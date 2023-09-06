package com.ariesvelasquez.androidmaps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import com.ariesvelasquez.androidmaps.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng


class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var binding: ActivityMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private var locationUtil: LocationUtil? = null
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenForPermissionRequests()
        initMap()
    }

    private fun listenForPermissionRequests() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.all { entry -> entry.value }) {
                initMap()
            } else {
                requestForLocationPermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkLocationSettings()
    }

    private fun checkLocationSettings() {
        if (isLocationSettingsEnabled()) {
            locationUtil?.restartLocationTracking()
        } else {
            locationUtil?.stopLocationTracking()
            showLocationNotAvailableMessage()
            zoomToDefaultOrLastLocation()
        }
    }

    private fun initMap() {
        if (hasLocationPermission()) {
            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        } else {
            requestForLocationPermission()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.isTrafficEnabled = true

        getCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            requestForLocationPermission()
            zoomToDefaultOrLastLocation()
            return
        }

        mMap?.isMyLocationEnabled = true

        val locationCallback = object : LocationCallback() {
            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.e(
                    "onLocationAvailability",
                    "onLocationAvailability ${availability.isLocationAvailable}"
                )
                if (!availability.isLocationAvailable) {
                    showLocationNotAvailableMessage()
                }
            }

            override fun onLocationResult(result: LocationResult) {
                // Handle when location is available
                result.lastLocation?.let {
                    lastLocation = it
                    mMap?.zoomCenter(it, 5f)
                }
            }
        }
        locationUtil = LocationUtil(this, locationCallback).apply {
            startLocationTracking()
        }
    }

    private fun zoomToDefaultOrLastLocation() {
        mMap?.apply {
            val location = lastLocation ?: quezonCityLocation
            zoomCenter(location)
            uiSettings.isMyLocationButtonEnabled = false
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestForLocationPermission() {
        showDialog(
            getString(R.string.location_permission_desc)
        ) {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            requestPermissionLauncher.launch(permissions)
        }
    }

    override fun onStop() {
        super.onStop()
        locationUtil?.stopLocationTracking()
    }

    private fun showDialog(
        message: String,
        positiveCallback: () -> Unit
    ) {
        AlertDialog.Builder(
            this
        ).setTitle(getString(R.string.location_permission_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.okay)) { _, _ ->
                positiveCallback.invoke()
            }.show()
    }

    private fun showLocationNotAvailableMessage() {
        Toast.makeText(
            this@MapsActivity,
            getString(R.string.location_unavailable_message),
            Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        val quezonCityLocation = Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = 14.6760
            longitude = 121.0437
        }
    }
}

fun GoogleMap.zoomCenter(location: Location, zoom: Float = 14.0.toFloat()) {
    this.animateCamera(
        CameraUpdateFactory.newLatLngZoom(
            LatLng(
                location.latitude,
                location.longitude
            ), 14.0.toFloat()
        )
    )
}

fun Activity.isLocationSettingsEnabled(): Boolean {
    val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}