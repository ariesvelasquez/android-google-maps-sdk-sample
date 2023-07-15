package com.ariesvelasquez.androidmaps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ariesvelasquez.androidmaps.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLocationServices()
        listenForPermissionRequests()
        initMapFragment()
    }

    private fun listenForPermissionRequests() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.all { entry -> entry.value }) {
                initLocationServices()
                initMapFragment()
            } else {
                requestForLocationPermission()
            }
        }
    }

    private fun initLocationServices() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initMapFragment() {
        if (hasLocationPermission()) {
            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        } else {
            requestForLocationPermission()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isTrafficEnabled = true

        if (hasLocationPermission()) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            zoomToDefaultLocation()
        }
    }

    private fun getCurrentLocation() {
        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(this) { task ->
                when {
                    task.isSuccessful -> {
                        lastKnownLocation = task.result
                        lastKnownLocation?.let {
                            mMap.zoomCenter(it)
                        }
                    }
                    else -> zoomToDefaultLocation()
                }
            }
        } catch (e: SecurityException) {
            Log.e("SecurityException", "Exception: ${e.message}")
            zoomToDefaultLocation()
        }
    }

    private fun zoomToDefaultLocation() {
        mMap.apply {
            zoomCenter(quezonCityLocation)
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

    private fun requestForLocationPermission(): Boolean {
        AlertDialog.Builder(
            this
        ).setTitle("Location Permission Required")
            .setMessage("This feature needs Location Permission, please accept the permission to proceed.")
            .setPositiveButton("Okay") { _, _ ->
                val permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                requestPermissionLauncher.launch(permissions)
            }.show()
        return false
    }

    companion object {
        val quezonCityLocation = Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = 14.6760
            longitude = 121.0437
        }
    }
}

fun GoogleMap.zoomCenter(location: Location, zoom: Float = 14.0.toFloat()) {
    this.moveCamera(
        CameraUpdateFactory.newLatLngZoom(
            LatLng(
                location.latitude,
                location.longitude
            ), 14.0.toFloat()
        )
    )
}