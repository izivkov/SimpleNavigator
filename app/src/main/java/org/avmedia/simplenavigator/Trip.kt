package org.avmedia.simplenavigator

import android.location.Location
import kotlin.math.absoluteValue

data class Trip(var lastLocation: Location = Location("dummyprovider")) {
    var distance: Float = 0f
    var topSpeed: Float = 0f
    var descent: Double = 0.0
    var ascent: Double = 0.0
    var steps: Int = 0
    var isPaused: Boolean = false
    val MIN_DISTANCE_ACCURACY:Float = 50f
    val MIN_ALTITUDE_ACCURACY:Float = 5f
    var lastLocationAltitude: Location = Location("dummyprovider")

    fun reset() {
        distance = 0f
        topSpeed = 0f
        descent = 0.0
        ascent = 0.0
        steps = 0
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

        if (lastLocation.altitude == 0.0 || isPaused) {
            lastLocation = newLocation
            return
        }

        val distanceDelta = newLocation.distanceTo(lastLocation)
        if (distanceDelta > this.MIN_DISTANCE_ACCURACY) {
            distance += distanceDelta
            lastLocation = newLocation // only set current location if used
        }

        if ((newLocation.speed * 3600 / 1000) > topSpeed) {
            topSpeed = newLocation.speed * 3600 / 1000
        }

        if (lastLocationAltitude.altitude == 0.0) {
            lastLocationAltitude = newLocation
            return
        }
        val altitudeDelta:Double = newLocation.altitude - lastLocationAltitude.altitude

        if (altitudeDelta.absoluteValue > MIN_ALTITUDE_ACCURACY) {
            if (altitudeDelta.compareTo(0.0) == 1) {
                ascent += altitudeDelta
            } else {
                descent += altitudeDelta.absoluteValue
            }

            lastLocationAltitude = newLocation
        }
    }
}
