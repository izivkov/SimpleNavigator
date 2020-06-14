package org.avmedia.simplenavigator

import android.location.Location
import android.util.Log
import kotlin.math.absoluteValue

data class Trip(var currentLocation: Location = Location("dummyprovider")) {
    var distance: Float = 0f
    var topSpeed: Float = 0f
    var descent: Double = 0.0
    var ascent: Double = 0.0
    var lastUsedAltitude: Double = -1000.0
    var isPaused: Boolean = false

    fun reset() {
        distance = 0f
        topSpeed = 0f
        descent = 0.0
        ascent = 0.0
        isPaused = false
    }

    private fun pause() {
        isPaused = true
    }

    private fun resume () {
        isPaused = false
    }

    fun togglePause () {
        if (isPaused) {
            resume()
        } else {
            pause()
        }
    }

    fun set(newLocation: Location) {
        if (currentLocation.altitude == 0.0 || isPaused) {
            currentLocation = newLocation
            return
        }

        val distanceDelta = newLocation.distanceTo(currentLocation)
        if (distanceDelta > newLocation.getAccuracy()) {
            distance += distanceDelta
        }

        if ((newLocation.speed * 3600 / 1000) > topSpeed) {
            topSpeed = newLocation.speed * 3600 / 1000
        }

        if (lastUsedAltitude == -1000.0) {
            lastUsedAltitude = newLocation.altitude
        }
        val altitudeDelta:Double = newLocation.altitude - lastUsedAltitude
        Log.d("Trip", "altitudeDelta: " + altitudeDelta+", newLocation.verticalAccuracyMeters:" + newLocation.verticalAccuracyMeters )

        if (!newLocation.hasAccuracy () || altitudeDelta > newLocation.verticalAccuracyMeters) {
            Log.d("Trip", "altitudeDelta: " + altitudeDelta)
            if (altitudeDelta > 0.0) {
                Log.d("Trip", "Adding to ascent")
                ascent += altitudeDelta
            } else {
                Log.d("Trip", "Adding to descent")
                descent += altitudeDelta.absoluteValue
            }
            lastUsedAltitude = newLocation.altitude
        }
        Log.d("Trip", "ascent: " + ascent + ", descent" + descent)

        currentLocation = newLocation
    }
}