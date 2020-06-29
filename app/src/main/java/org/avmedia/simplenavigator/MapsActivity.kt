package org.avmedia.simplenavigator

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
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
import org.avmedia.simplenavigator.activityrecognition.ActivityCallback
import org.avmedia.simplenavigator.activityrecognition.ActivityCallbackAbstract
import org.avmedia.simplenavigator.activityrecognition.TransitionRecognition
import org.avmedia.simplenavigator.nearby.ConnectionCallback
import org.avmedia.simplenavigator.nearby.ConnectionsCallbackAbstract
import org.avmedia.simplenavigator.nearby.NearbyConnection
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 45

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

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
    private lateinit var mTransitionRecognition: TransitionRecognition
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

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
        setupNewLocationHandler()

        ActivityCallback.callback = object : ActivityCallbackAbstract() {
            override fun update(newValue: String) {
                val res = when (newValue) {
                    "STILL" -> R.drawable.ic_still
                    "WALKING" -> R.drawable.ic_directions_walk_24px
                    "ON_FOOT" -> R.drawable.ic_directions_walk_24px
                    "RUNNING" -> R.drawable.ic_directions_run_24px
                    "ON_BICYCLE" -> R.drawable.ic_directions_bike_24px
                    "IN_VEHICLE" -> R.drawable.ic_directions_car_24px
                    else -> R.drawable.ic_directions_blank
                }

                val activityImage: ImageView = findViewById(R.id.activity_image)
                activityImage.setImageResource(res)
            }
        }

        ConnectionCallback.callback = object : ConnectionsCallbackAbstract() {
            override fun update(status: String) {
                val pairButton: Button = findViewById(R.id.pair)
                if (pairButton != null && pairButton.animation != null) {
                    pairButton.animation.cancel()
                }
                Toast.makeText(
                    applicationContext,
                    "Connection status: " + status,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        createLocationRequest()
        initTransitionRecognition()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun initTransitionRecognition() {
        mTransitionRecognition = TransitionRecognition()
    }

    fun setupNewLocationHandler() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation

                if (!unitConverter.isInitialized) {
                    unitConverter.init(applicationContext, lastLocation)
                }

                val latLong = LatLng(lastLocation.latitude, lastLocation.longitude)
                placeMarkerOnMap()

                val currentZoom: Float
                if (map.cameraPosition.zoom >= 8.0) {
                    currentZoom = map.cameraPosition.zoom
                } else {
                    currentZoom = DEFAULT_ZOOM
                }

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, currentZoom))

                setSpeedometer()
            }
        }
    }

    private fun setSpeedometer() {

        val speedView: TextView = findViewById<TextView>(R.id.speed)
        speedView.text = unitConverter.formatSpeed((lastLocation.speed * 3600 / 1000), "")

        val altView: TextView = findViewById<TextView>(R.id.altitude)
        altView.text = unitConverter.formatMeters(lastLocation.altitude, "Altitude:")

        trip.set(lastLocation)
        displayTrip()
    }

    fun showYesNoTripResetDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Trip Reset")
        builder.setMessage("Are you sure you like to reset the data for the current trip?")
        builder.setPositiveButton("Yes") { dialog, which ->
            trip.reset()
            displayTrip()
            Toast.makeText(applicationContext, "Trip data reset", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No") { dialog, which ->
        }

        builder.show()
    }

    fun showYesNoExitDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exiting App")
        builder.setMessage("Are you sure you like to exit? Trip data will be lost")
        builder.setPositiveButton("Yes") { dialog, which ->
            this.finish()
        }
        builder.setNegativeButton("No") { dialog, which ->
        }

        builder.show()
    }

    fun setupButtons() {
        val resetButton: Button = findViewById(R.id.resetTripButton)
        resetButton.setOnClickListener {
            showYesNoTripResetDialog()
        }

        val pauseResumeButton: ImageButton = findViewById(R.id.pauseResumeButton)
        pauseResumeButton.setOnClickListener {
            trip.togglePause()

            val anim: Animation = AlphaAnimation(0.6f, 1.0f)
            anim.duration = 500 //You can manage the blinking time with this parameter

            anim.startOffset = 20
            anim.repeatMode = Animation.REVERSE
            anim.repeatCount = Animation.INFINITE
            val tripPanel: RelativeLayout = findViewById(R.id.relativeLayoutTrip)
            val starResumeButton: ImageButton = findViewById(R.id.pauseResumeButton)

            if (trip.isPaused) {
                tripPanel.startAnimation(anim)
                starResumeButton.startAnimation(anim)
                pauseResumeButton.setImageResource(R.drawable.ic_play_arrow_24px)
                Toast.makeText(applicationContext, "Trip paused", Toast.LENGTH_SHORT).show()
            } else {
                tripPanel.animation.cancel()
                starResumeButton.animation.cancel()
                pauseResumeButton.setImageResource(R.drawable.ic_pause_circle_outline_24px)
                Toast.makeText(applicationContext, "Trip resumed", Toast.LENGTH_SHORT).show()
            }
        }

        val exitButton: Button = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            showYesNoExitDialog()
        }
        val infoButton: ImageButton = findViewById(R.id.info)
        infoButton.setOnClickListener {
            showInfoDialog()
        }
        val pairButton: Button = findViewById(R.id.pair)
        pairButton.setOnClickListener {
            if (!NearbyConnection.connecting) {
                NearbyConnection.connect(this)

                val anim: Animation = AlphaAnimation(0.2f, 1.0f)
                anim.duration = 500 //You can manage the blinking time with this parameter
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = Animation.INFINITE
                pairButton.startAnimation(anim)
            } else {
                NearbyConnection.stopTryingToConnect()
                if (pairButton != null && pairButton.animation != null) {
                    pairButton.animation.cancel()
                }
            }
        }
    }

    fun showInfoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pairing Devices")
        val msg =
            Html.fromHtml(
                """
<i>Pairing</i> devices allows you to see each other's location.

<li>Run the app on another device.</li>
<li>Press the <b>Pair</b> buttons on both devices at the same time.</li>
<li>The devices will connect to each other.</li>
</br>
This is useful for group rides, locating a person in a mall, hiking with a group, etc.
""", Html.FROM_HTML_MODE_LEGACY
            )

        builder.setMessage(msg)
        builder.setPositiveButton("Got It") { dialog, which ->
        }

        builder.show()
    }


    fun displayTrip() {
        val distanceView: TextView = findViewById<TextView>(R.id.distance)
        distanceView.text = unitConverter.formatKm(trip.distance / 1000, "Distance:")

        val topSpeed: TextView = findViewById<TextView>(R.id.topSpeed)
        topSpeed.text = unitConverter.formatSpeed(trip.topSpeed, "Top Speed:")

        val ascent: TextView = findViewById<TextView>(R.id.ascent)
        ascent.text = unitConverter.formatMeters(trip.ascent, "Ascent:")

        val descent: TextView = findViewById<TextView>(R.id.descent)
        descent.text = unitConverter.formatMeters(trip.descent, "Descent:")
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
        map.isTrafficEnabled = true

        getPermissions()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }

    private fun getPermissions() {

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                // arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUIRED_PERMISSIONS,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            map.isMyLocationEnabled = true
            map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(0.0, 0.0)))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        val permissionResult = "Request code: " + requestCode + ", Permissions: " +
                Arrays.toString(permissions) + ", Results: " + Arrays.toString(
            grantResults
        )
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
            }

            if (runningQOrLater) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                        PERMISSION_REQUEST_ACTIVITY_RECOGNITION
                    )
                }
            }
        }
    }

    private fun placeMarkerOnMap() {
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
        mTransitionRecognition.stopTracking()
        NearbyConnection.shutdownConnection()
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
        mTransitionRecognition.startTracking(this)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 3000
        locationRequest.fastestInterval = 3000
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