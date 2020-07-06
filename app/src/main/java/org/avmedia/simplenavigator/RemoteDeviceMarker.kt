package org.avmedia.simplenavigator

import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.avmedia.simplenavigator.firebase.ShareLocationMessage
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
        })
    }

    // If we don't get an update within 10 seconds, hide marker.
    private fun resetTimer() {
        timer?.cancel()
        timer = Timer("Make Invisible", true).schedule(12000) {
            Log.d("resetTimer", "Making marker invisible")
            activity.runOnUiThread(java.lang.Runnable {
                marker.isVisible = false
            })
        }
    }
}
