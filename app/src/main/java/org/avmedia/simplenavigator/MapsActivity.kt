package org.avmedia.simplenavigator

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private var unitConverter = UnitConverter()
    }

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    private val DEFAULT_ZOOM = 16.0f
    private val trip = Trip()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupButtons()
        setupNewLocationHandler ()

        createLocationRequest()

        // val transitions = mutableListOf<ActivityTransition>()
        // ActivityTransitionRequest(transitions)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun setupNewLocationHandler () {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation

                if (!Companion.unitConverter.isInitialized) {
                    Companion.unitConverter.init(applicationContext, lastLocation)
                }

                val latLong = LatLng(lastLocation.latitude, lastLocation.longitude)
                placeMarkerOnMap(latLong)

                var currentZoom = 0f
                if (map.getCameraPosition().zoom >= 8.0) {
                    currentZoom = map.getCameraPosition().zoom
                } else {
                    currentZoom = DEFAULT_ZOOM
                }

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, currentZoom))

                val speedView: TextView = findViewById(R.id.speed) as TextView
                speedView.setText(unitConverter.formatSpeed((lastLocation.speed * 3600 / 1000), ""))

                val altView: TextView = findViewById(R.id.altitude) as TextView
                altView.setText(unitConverter.formatMeters(lastLocation.altitude, "Altitude:"))

                trip.set(lastLocation)
                displayTrip()
            }
        }
    }

    fun setupButtons () {
        val resetButton: Button = findViewById(R.id.resetTripButton)
        resetButton.setOnClickListener {
            trip.reset()
            displayTrip()
        }

        val pauseResumeButton: ImageButton = findViewById(R.id.pauseResumeButton)
        pauseResumeButton.setOnClickListener {
            trip.togglePause()

            if (trip.isPaused) {
                pauseResumeButton.setImageResource(R.drawable.ic_play_arrow_24px)
            } else {
                pauseResumeButton.setImageResource(R.drawable.ic_pause_circle_outline_24px)
            }
        }

        val exitButton: Button = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            this.finish()
        }
    }

    fun displayTrip() {
        val distanceView: TextView = findViewById(R.id.distance) as TextView
        distanceView.setText(unitConverter.formatKm(trip.distance/1000, "Distance:"))

        val topSpeed: TextView = findViewById(R.id.topSpeed) as TextView
        topSpeed.setText(unitConverter.formatSpeed(trip.topSpeed, "Top Speed:"))

        val ascent: TextView = findViewById(R.id.ascent) as TextView
        ascent.setText(unitConverter.formatMeters(trip.ascent, "Ascent:"))

        val descent: TextView = findViewById(R.id.descent) as TextView
        descent.setText(unitConverter.formatMeters(trip.descent, "Descent:"))
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)
        map.setTrafficEnabled(true)

        setUpMap()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        map.isMyLocationEnabled = true

        map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(0.0, 0.0)))
    }

    private fun placeMarkerOnMap(location: LatLng) {
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    // 1
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        locationUpdateState = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this@MapsActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }
}