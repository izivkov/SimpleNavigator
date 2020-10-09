package org.avmedia.simplenavigator

import RouteTracker
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import org.avmedia.simplenavigator.EventProcessor.ProgressEvents.*
import org.avmedia.simplenavigator.activityrecognition.TransitionRecognition
import org.avmedia.simplenavigator.firebase.FirebaseConnection
import org.avmedia.simplenavigator.firebase.ShareLocationMessage
import org.avmedia.simplenavigator.nearby.NearbyConnection
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 45
        private const val PERMISSION_REQUEST_CONTACTS = 20

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        private const val REQUEST_CHECK_SETTINGS = 2
        private var unitConverter = UnitConverter()
    }

    private lateinit var reviewInfo: ReviewInfo
    private lateinit var reviewManager: ReviewManager

    private var locationActive: Boolean = false
    private lateinit var routeTracker: RouteTracker
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private val DEFAULT_ZOOM = 16.0f
    private val trip = Trip()
    private lateinit var mTransitionRecognition: TransitionRecognition
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    var currentActivity: String = "UNKNOWN"
    lateinit var remoteMarker: RemoteDeviceMarker
    lateinit var stepCounter: StepCounter

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

        // createLocationRequest()
        initStepsCounter()

        createAppEventsSubscription()
        PairConnection.init()
        initTransitionRecognition()
        initRating()
        displayVersion()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun displayVersion() {
        val versionName = BuildConfig.VERSION_NAME

        val versionText: TextView = findViewById(R.id.version)
        versionText.text = versionName
    }

    private fun initRating() {
        reviewManager = ReviewManagerFactory.create(applicationContext)
    }

    private fun askForReview() {

        val requestFlow = reviewManager.requestReviewFlow()
        requestFlow.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                //Received ReviewInfo object
                reviewInfo = request.result
                val reviewInfo = request.result
                val flow = reviewManager.launchReviewFlow(this@MapsActivity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.

                    this.finish()
                }
            } else {
                // something went wrong
                this.finish()
            }
        }
    }

    private fun createAppEventsSubscription(): Disposable =
        EventProcessor.connectionEventFlowable
            .observeOn(PairConnection.whereToRun)
            .doOnNext {
                when (it) {
                    // Nearby
                    NearbyConnectionSuccess -> {
                        PairConnection.connectionStatus =
                            PairConnection.ConnectionStatus.NEARBY_CONNECTED

                        Toast.makeText(
                            applicationContext,
                            "Connection status: " + "SUCCESS",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    NearbyConnectionFailed -> {
                        val pairButton: Button = findViewById(R.id.pair)
                        Toast.makeText(
                            applicationContext,
                            "Connection status: " + "FAILED",
                            Toast.LENGTH_LONG
                        ).show()

                        resetPairButton()
                    }

                    EventProcessor.ProgressEvents.NearbyConnecting -> {
                        PairConnection.connectionStatus =
                            PairConnection.ConnectionStatus.NEARBY_CONNECTING
                    }

                    EventProcessor.ProgressEvents.NearbyConnectionPayload -> {
                        PairConnection.currentTopic = "" + it.payload

                        NearbyConnection.disconnect()

                        FirebaseConnection.subscribe(PairConnection.currentTopic)
                        PairConnection.connectionStatus =
                            PairConnection.ConnectionStatus.SUBSCRIBING_TO_TOPIC
                    }

                    // Firebase
                    SubscribedToFirebaseFailed -> {
                        resetPairButton()

                        Toast.makeText(
                            applicationContext,
                            "Subscription to Message FAILED",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    SubscribedToFirebaseSuccess -> {

                        PairConnection.connectionStatus =
                            PairConnection.ConnectionStatus.SUBSCRIBED_TO_TOPIC

                        val pairButton: Button = findViewById(R.id.pair)
                        pairButton.animation?.cancel()
                        pairButton.setBackgroundResource(R.drawable.button_background_red)
                        pairButton.setText(R.string.Unpair)

                        Toast.makeText(
                            applicationContext,
                            "Subscription to Message SUCCESSFUL",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    PairedDeviceLocation -> {
                        val remoteLocationStr = it.payload
                        val remoteLocation: ShareLocationMessage =
                            Gson().fromJson(remoteLocationStr, ShareLocationMessage::class.java)

                        remoteMarker.update(remoteLocation)
                    }

                    CloseAllConnection -> {
                        Toast.makeText(
                            applicationContext,
                            "Un-paired",
                            Toast.LENGTH_LONG
                        ).show()
                        resetPairButton()

                        if (PairConnection.connectionStatus in PairConnection.ConnectionStatus.NEARBY_CONNECTED..PairConnection.ConnectionStatus.SUBSCRIBED_TO_TOPIC) {
                            NearbyConnection.disconnect()
                        }
                        FirebaseConnection.unsubscribe(PairConnection.currentTopic)
                        PairConnection.connectionStatus =
                            PairConnection.ConnectionStatus.DISCONNECTED
                    }

                    // General
                    ActivityChangeEvent -> {
                        currentActivity = it.payload

                        val res = when (currentActivity) {
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

                        routeTracker.newActivity(currentActivity, this@MapsActivity, map)
                    }

                    StepsChangeEvent -> {
                        trip.steps = it.payload.toInt()
                    }
                }
            }
            .subscribe(
                { },
                { throwable ->
                    Log.d(
                        "createAppEventsSubscription",
                        "Got error on subscribe: $throwable"
                    )
                })

    fun initTransitionRecognition() {
        mTransitionRecognition = TransitionRecognition()
    }

    fun initStepsCounter() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounter = StepCounter(sensorManager)
    }

    fun setupNewLocationHandler() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {

                // XXX location update

                super.onLocationResult(p0)

                lastLocation = p0.lastLocation

                if (!unitConverter.isInitialized) {
                    unitConverter.init(applicationContext, lastLocation)
                }

                val latLong = LatLng(lastLocation.latitude, lastLocation.longitude)

                val currentZoom: Float
                if (map.cameraPosition.zoom >= 8.0) {
                    currentZoom = map.cameraPosition.zoom
                } else {
                    currentZoom = DEFAULT_ZOOM
                }

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, currentZoom))

                setSpeedometer()

                if (PairConnection.connectionStatus == PairConnection.ConnectionStatus.SUBSCRIBED_TO_TOPIC) {
                    PairConnection.send(
                        ShareLocationMessage(
                            lastLocation.longitude,
                            lastLocation.latitude,
                            currentActivity,
                            User.getUser(applicationContext)
                        )
                    )
                }

                routeTracker.add(latLong, map)
            }
        }
    }

    private fun setSpeedometer() {

        val speedView: TextView = findViewById<TextView>(R.id.speed)
        speedView.text = unitConverter.formatSpeedHtml((lastLocation.speed * 3600 / 1000), "")

        val altView: TextView = findViewById<TextView>(R.id.altitude)
        altView.text = unitConverter.formatMeters(lastLocation.altitude, "Elevation:")

        trip.set(lastLocation)
        displayTrip()
    }

    fun showYesNoTripResetDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Trip Reset")
        builder.setMessage("Are you sure you like to reset the data for the current trip?")
        builder.setPositiveButton("Yes") { dialog, which ->
            trip.reset()
            routeTracker.clear()
            stepCounter.clear()
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
            fusedLocationClient.removeLocationUpdates(locationCallback)
            mTransitionRecognition.stopTracking()

            routeTracker.clear()
            stepCounter.clear()

            askForReview()

            // this.finish()
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
            routeTracker.togglePause()

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

            when (PairConnection.connectionStatus) {
                PairConnection.ConnectionStatus.DISCONNECTED -> {
                    PairConnection.nearbyConnect(this)

                    val anim: Animation = AlphaAnimation(0.2f, 1.0f)
                    anim.duration = 500 //You can manage the blinking time with this parameter
                    anim.repeatMode = Animation.REVERSE
                    anim.repeatCount = Animation.INFINITE
                    pairButton.startAnimation(anim)
                }

                in (PairConnection.ConnectionStatus.NEARBY_CONNECTED..PairConnection.ConnectionStatus.SUBSCRIBED_TO_TOPIC) -> {
                    PairConnection.disconnect()
                    resetPairButton()
                }

                PairConnection.ConnectionStatus.GOT_FIRST_PAYLOAD -> {
                    PairConnection.disconnect()
                    resetPairButton()
                }

                else -> {
                    resetPairButton()
                    PairConnection.connectionStatus = PairConnection.ConnectionStatus.DISCONNECTED
                }
            }
        }
    }

    private fun resetPairButton() {
        val pairButton: Button = findViewById(R.id.pair)

        pairButton.animation?.cancel()
        pairButton.setBackgroundResource(R.drawable.button_background_green)
        pairButton.text = "Pair"
    }

    fun showInfoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pairing Devices")
        val msg =
            Html.fromHtml(
                """
<i>Pairing</i> devices allows you to see each other's location and current activity.

<li>Run the app on another device.</li>
<li>Press the <b>Pair</b> buttons on both devices at the same time.</li>
<li>The devices will connect to each other. (Be patient, this may take up to a minute)</li>
<li>When the <b>PAIR</b> button stops blinking, and turns red, the devices are paired</li>
</br>
This is useful for group rides, locating a person in a mall, hiking with a group, etc.
""", Html.FROM_HTML_MODE_LEGACY
            )

        builder.setMessage(msg)
        builder.setPositiveButton("Got It") { dialog, which ->
        }

        builder.show()
    }

    @SuppressLint("SetTextI18n")
    fun displayTrip() {
        val distanceView: TextView = findViewById<TextView>(R.id.distance)
        distanceView.text = unitConverter.formatKm(trip.distance / 1000, "Distance:")

        val topSpeed: TextView = findViewById<TextView>(R.id.topSpeed)
        topSpeed.text = unitConverter.formatSpeed(trip.topSpeed, "Top Speed:")

        val verticalDistanceTravelled: TextView =
            findViewById<TextView>(R.id.verticalDistanceTravelled)

        val ascentStr = Html.fromHtml(
            unitConverter.formatMeters(trip.ascent, "<b>↑</b>") + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                    unitConverter.formatMeters(trip.descent, "<b>↓</b>"),
            Html.FROM_HTML_MODE_LEGACY
        )

        verticalDistanceTravelled.text = ascentStr

        val stepCounter: TextView =
            findViewById<TextView>(R.id.stepCounter)
        stepCounter.text = java.lang.String.format("%s: %d", "Steps", trip.steps)
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

        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        map.setOnMarkerClickListener(this)
        map.isTrafficEnabled = true

        val drawable = ContextCompat.getDrawable(this@MapsActivity, R.drawable.ic_navigation_24px)
        remoteMarker = RemoteDeviceMarker(map, this@MapsActivity)

        routeTracker = RouteTracker()

        getPermissions()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }

    private fun getPermissions() {
        // checkContactPermissions ()

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

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null /* Looper */
                )
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
        if (requestCode == PERMISSION_REQUEST_CONTACTS) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    PERMISSION_REQUEST_CONTACTS
                )
            }
        }
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

    public override fun onPause() {
        super.onPause()
        mTransitionRecognition.stopTracking()

        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationActive = false
    }

    public override fun onResume() {
        super.onResume()
        mTransitionRecognition.startTracking(this)

        if (!locationActive) {
            createLocationRequest()
            locationActive = true
        }
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
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                Log.d("", "addOnFailureListener, error: {}", e)
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
                    Log.d("", "SendIntentException: addOnFailureListener, error: {}", e)
                }
            }
        }
    }
}