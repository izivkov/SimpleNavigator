import android.graphics.Color
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import org.avmedia.simplenavigator.MapsActivity
import org.avmedia.simplenavigator.R
import org.avmedia.simplenavigator.utils.Utils.getMarkerIconFromDrawable
import kotlin.random.Random

class RouteTracker {

    lateinit var lineRoute: Polyline
    private val activityMarkers = mutableListOf<Marker>()
    private var lastTime = 0L
    private var isPaused = false
    private var lastActivity: String = "UNKNOWN"

    fun add(newPoint: LatLng, googleMap: GoogleMap) {
        val MAX_POINTS = 1000

        if (!this::lineRoute.isInitialized) {
            lineRoute = googleMap.addPolyline(PolylineOptions().clickable(false))
            stylePolyline(lineRoute)
        }

        if (!isPaused && isTimeToShow() && isFarEnough(newPoint)) {

            if (lineRoute.points.size >= MAX_POINTS) {
                lineRoute.points.removeAt(
                    rand(
                        1,
                        lineRoute.points.size - 1
                    )
                ) // remove a random point
            }

            val points = lineRoute.points
            points.add(newPoint)
            lineRoute.points = points
        }
    }

    fun clear() {
        if (this::lineRoute.isInitialized) {
            val points = lineRoute.points
            points.clear()
            lineRoute.points = points
        }

        lastTime = 0L

        for (marker in activityMarkers) {
            marker.remove()
        }
        activityMarkers.clear()

        lastActivity = "UNKNOWN"
    }

    fun togglePause() {
        if (isPaused) {
            resume()
        } else {
            pause()
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun newActivity(activity: String, mapsActivity: MapsActivity, googleMap: GoogleMap) {
        if (!this::lineRoute.isInitialized) {
            return
        }

        if (activity == "STILL" || lastActivity == activity) {
            return
        }

        val res = when (activity) {
            "WALKING" -> R.drawable.activity_change_walking
            "ON_FOOT" -> R.drawable.activity_change_walking
            "RUNNING" -> R.drawable.activity_change_running
            "ON_BICYCLE" -> R.drawable.activity_change_bike
            "IN_VEHICLE" -> R.drawable.activity_change_in_vehicle
            else -> R.drawable.marker_unknown
        }

        lastActivity = activity

        val lastPoint = lineRoute.points.last()
        var marker = googleMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    lastPoint.latitude,
                    lastPoint.longitude
                )
            )
        )

        val drawable = ContextCompat.getDrawable(mapsActivity, res)
        val markerIcon = getMarkerIconFromDrawable(drawable)

        marker.setIcon(markerIcon)

        marker.isVisible = true
        marker.showInfoWindow()
        marker.zIndex = -1f

        activityMarkers.add(marker)
    }

    // support functions
    private fun isFarEnough(newPoint: LatLng): Boolean {
        val MIN_DISTANCE_FROM_LAST_POINT = 10

        if (!this::lineRoute.isInitialized || lineRoute.points == null || lineRoute.points.size < 1) {
            return true
        }

        val lastLongLat = lineRoute.points.last()
        val lastLocation = Location("")
        lastLocation.latitude = lastLongLat.latitude
        lastLocation.longitude = lastLongLat.longitude

        val newLocation = Location("")
        newLocation.longitude = newPoint.longitude
        newLocation.latitude = newPoint.latitude

        var distance = lastLocation.distanceTo(newLocation)
        return distance > MIN_DISTANCE_FROM_LAST_POINT
    }

    private fun rand(start: Int, end: Int): Int {
        require(!(start > end || end - start + 1 > Int.MAX_VALUE)) { "Illegal Argument" }
        return Random(System.nanoTime()).nextInt(end - start + 1) + start
    }

    private fun isTimeToShow(): Boolean {
        val MIN_TIME_BETWEEN_POINTS = 10 * 1000
        val now = System.currentTimeMillis()

        val timeToShow = (now - lastTime) > MIN_TIME_BETWEEN_POINTS
        if (timeToShow) {
            lastTime = now
        }
        return timeToShow
    }

    private fun stylePolyline(polyline: Polyline?) {
        if (polyline == null) {
            return
        }

        val PATTERN_DASH_LENGTH_PX = 20
        val PATTERN_GAP_LENGTH_PX = 20
        val POLYLINE_STROKE_WIDTH_PX = 24

        val DOT: PatternItem = Dot()
        val GAP: PatternItem = Gap(PATTERN_GAP_LENGTH_PX.toFloat())
        val DASH: PatternItem = Dash(PATTERN_DASH_LENGTH_PX.toFloat())

        var pattern = listOf(GAP, DOT)
        var strokeColor = Color.BLUE

        val type = polyline.tag?.toString() ?: ""
        polyline.endCap = RoundCap()
        polyline.width = POLYLINE_STROKE_WIDTH_PX.toFloat()
        polyline.color = strokeColor
        polyline.jointType = JointType.ROUND
        polyline.pattern = pattern
    }
}