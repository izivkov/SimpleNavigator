package org.avmedia.simplenavigator

import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.avmedia.simplenavigator.firebase.ShareLocationMessage
import org.avmedia.simplenavigator.utils.Utils
import java.util.*
import kotlin.concurrent.schedule


data class RemoteDeviceMarker(var map: GoogleMap, var activity: MapsActivity) {

    var marker = map.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)))
    var timer: TimerTask? = null

    init {
        marker.isVisible = false
    }

    fun update(locationMsg: ShareLocationMessage) {
        activity.runOnUiThread(java.lang.Runnable {

            resetTimer()
            marker.isVisible = true

            val longLat = LatLng(locationMsg.latitude, locationMsg.longitude)
            marker.position = longLat
            marker.title = locationMsg.userName
            marker.showInfoWindow()
            marker.zIndex = -1f

            val res = when (locationMsg.activity) {
                "STILL" -> R.drawable.marker_in_still
                "WALKING" -> R.drawable.marker_walking
                "ON_FOOT" -> R.drawable.marker_walking
                "RUNNING" -> R.drawable.marker_running
                "ON_BICYCLE" -> R.drawable.marker_bike
                "IN_VEHICLE" -> R.drawable.marker_in_vehicle
                else -> R.drawable.marker_unknown
            }

            val drawable = ContextCompat.getDrawable(activity, res)
            val markerIcon = Utils.getMarkerIconFromDrawable(drawable)

            marker.setIcon(markerIcon)
        })
    }

    // If we don't get an update within 10 seconds, hide marker.
    private fun resetTimer() {
        timer?.cancel()
        timer = Timer("Make Invisible", true).schedule(12000) {
            activity.runOnUiThread(java.lang.Runnable {
                marker.isVisible = false
            })
        }
    }
}
