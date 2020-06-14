package org.avmedia.simplenavigator

import android.location.Location
import android.util.Log
import kotlin.math.absoluteValue

data class Trip(var currentLocation: Location = Location("dummyprovider")) {
    var distance: Float = 0f
    var topSpeed: Float = 0f
    var descent: Double = 0.0
    var ascent: Double = 0.0
    var isPaused: Boolean = false
    var currentLocationVertical: Location = Location("dummyprovider")

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
            currentLocationVertical = newLocation
            return
        }

        val distanceDelta = newLocation.distanceTo(currentLocation)
        if (distanceDelta > newLocation.getAccuracy()) {
            distance += distanceDelta
        }

        if ((newLocation.speed * 3600 / 1000) > topSpeed) {
            topSpeed = newLocation.speed * 3600 / 1000
        }

        val altitudeDelta:Double = newLocation.altitude - currentLocationVertical.altitude
        Log.d("Trip", "altitudeDelta: " + altitudeDelta)

        if (!newLocation.hasAccuracy () || altitudeDelta.absoluteValue > newLocation.verticalAccuracyMeters) {
            if (altitudeDelta > 0.0) {
                Log.d("Trip", "Adding to ascent")
                ascent += altitudeDelta
            } else {
                Log.d("Trip", "Adding to descent")
                descent += altitudeDelta.absoluteValue
            }

            currentLocationVertical = newLocation
        }

        // do not set currentLocationVertical if not used
        Log.d("Trip", "ascent: " + ascent + ", descent: " + descent)
    }
}